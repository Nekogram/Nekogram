"""Pure-Python wheel installer for plugin __requirements__ metadata.

For safety and compatibility this intentionally accepts only wheels whose ABI and platform
are both "none" and "any". Native extensions are not installable into the app's Python runtime.
"""

import ast
import email.parser
import hashlib
import importlib.metadata
import os
import platform
import posixpath
import shutil
import sys
import tempfile
import urllib.parse
import urllib.request
import zipfile

from packaging.markers import default_environment
from packaging.requirements import Requirement
from packaging.specifiers import SpecifierSet
from packaging.utils import canonicalize_name, parse_wheel_filename
from packaging.version import Version

MAX_REQUIREMENTS_FILE_SIZE = 5 * 1024 * 1024
MAX_METADATA_SIZE = 15 * 1024 * 1024
MAX_WHEEL_SIZE = 30 * 1024 * 1024
MAX_TOTAL_WHEEL_SIZE = 120 * 1024 * 1024
MAX_UNPACKED_SIZE = 120 * 1024 * 1024
USER_AGENT = "Irongram-Python-Plugin-Manager/1.0"


def _read_plugin_requirements(plugin_path):
    with open(plugin_path, "rb") as plugin_file:
        source = plugin_file.read(MAX_REQUIREMENTS_FILE_SIZE + 1)
    if len(source) > MAX_REQUIREMENTS_FILE_SIZE:
        raise ValueError("Plugin source is larger than 5 MB")
    module = ast.parse(source.decode("utf-8-sig"), filename=plugin_path)
    for node in module.body:
        value = None
        if isinstance(node, ast.Assign):
            for target in node.targets:
                if isinstance(target, ast.Name) and target.id == "__requirements__":
                    value = node.value
                    break
        elif isinstance(node, ast.AnnAssign) and isinstance(node.target, ast.Name):
            if node.target.id == "__requirements__":
                value = node.value
        if value is not None:
            requirements = ast.literal_eval(value)
            if not isinstance(requirements, (list, tuple)):
                raise ValueError("__requirements__ must be a list of PEP 508 strings")
            if not all(isinstance(item, str) for item in requirements):
                raise ValueError("Every plugin dependency must be a string")
            return list(requirements)
    return []


def _marker_environment():
    environment = default_environment()
    environment["python_version"] = "{}.{}".format(sys.version_info.major, sys.version_info.minor)
    environment["python_full_version"] = "{}.{}.{}".format(
        sys.version_info.major, sys.version_info.minor, sys.version_info.micro
    )
    environment["sys_platform"] = "android"
    environment["platform_system"] = "Android"
    environment["platform_machine"] = platform.machine()
    return environment


def _requirement_applies(requirement, environment):
    if requirement.marker is None:
        return True
    return requirement.marker.evaluate(environment=environment)


def _installed_versions(library_directory):
    installed = {}
    os.makedirs(library_directory, exist_ok=True)
    for name in os.listdir(library_directory):
        if not name.endswith(".dist-info"):
            continue
        metadata_path = os.path.join(library_directory, name, "METADATA")
        if not os.path.isfile(metadata_path):
            continue
        try:
            with open(metadata_path, "rb") as metadata_file:
                metadata = email.parser.BytesParser().parse(metadata_file)
            package_name = metadata.get("Name")
            package_version = metadata.get("Version")
            if package_name and package_version:
                installed[canonicalize_name(package_name)] = Version(package_version)
        except Exception:
            continue
    return installed


def _bundled_version(name):
    try:
        return Version(importlib.metadata.version(name))
    except importlib.metadata.PackageNotFoundError:
        return None
    except Exception:
        return None


def _wheel_is_pure(filename):
    try:
        _, _, _, tags = parse_wheel_filename(filename)
    except Exception:
        return False
    supported_python_tags = {
        "py2", "py3", "py2.py3",
        "py{}{}".format(sys.version_info.major, sys.version_info.minor),
        "cp{}{}".format(sys.version_info.major, sys.version_info.minor),
    }
    return any(
        tag.platform == "any"
        and tag.abi == "none"
        and tag.interpreter in supported_python_tags
        for tag in tags
    )


def _fetch_json(url):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=45) as response:
        data = response.read(MAX_METADATA_SIZE + 1)
    if len(data) > MAX_METADATA_SIZE:
        raise ValueError("Package metadata is unexpectedly large")
    return __import__("json").loads(data.decode("utf-8"))


def _choose_wheel(package_name, specifiers, json_cache):
    cache_key = canonicalize_name(package_name)
    if cache_key not in json_cache:
        encoded_name = urllib.parse.quote(package_name, safe="")
        json_cache[cache_key] = _fetch_json("https://pypi.org/pypi/{}/json".format(encoded_name))
    project = json_cache[cache_key]

    choices = []
    for release, files in project.get("releases", {}).items():
        try:
            version = Version(release)
        except Exception:
            continue
        if any(not specifier.contains(version) for specifier in specifiers):
            continue
        for file_info in files:
            filename = file_info.get("filename", "")
            if not filename.endswith(".whl") or file_info.get("yanked"):
                continue
            if not _wheel_is_pure(filename):
                continue
            digest = file_info.get("digests", {}).get("sha256")
            if not digest or not file_info.get("url"):
                continue
            choices.append((version, filename, file_info["url"], digest))
    if not choices:
        requested = ", ".join(str(specifier) or "any" for specifier in specifiers) or "any"
        raise RuntimeError(
            "No compatible pure-Python wheel for {} ({})".format(package_name, requested)
        )
    return max(choices, key=lambda choice: choice[0])


def _download_wheel(url, expected_sha256, destination):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    digest = hashlib.sha256()
    total = 0
    with urllib.request.urlopen(request, timeout=90) as response, open(destination, "wb") as output:
        while True:
            chunk = response.read(1024 * 256)
            if not chunk:
                break
            total += len(chunk)
            if total > MAX_WHEEL_SIZE:
                raise ValueError("A dependency wheel is larger than 30 MB")
            digest.update(chunk)
            output.write(chunk)
    if digest.hexdigest().lower() != expected_sha256.lower():
        raise ValueError("SHA-256 mismatch while downloading a Python dependency")
    return total


def _read_wheel_metadata(wheel_path):
    with zipfile.ZipFile(wheel_path) as wheel:
        metadata_files = [
            name for name in wheel.namelist()
            if len(name.split("/")) == 2
            and name.split("/", 1)[0].endswith(".dist-info")
            and name.split("/", 1)[1] == "METADATA"
        ]
        if not metadata_files:
            raise ValueError("Dependency wheel is missing METADATA")
        raw = wheel.read(metadata_files[0])
        metadata = email.parser.BytesParser().parsebytes(raw)
        if not metadata.get("Name") or not metadata.get("Version"):
            raise ValueError("Dependency wheel metadata is incomplete")
        wheel_files = wheel.namelist()
        wheel_metadata_files = [name for name in wheel_files if name.endswith(".dist-info/WHEEL")]
        if wheel_metadata_files:
            wheel_header = email.parser.BytesParser().parsebytes(wheel.read(wheel_metadata_files[0]))
            if str(wheel_header.get("Root-Is-Purelib", "true")).lower() != "true":
                raise ValueError("The selected dependency wheel is not pure Python")
        return metadata


def _resolve(requirement_strings, library_directory, temporary_directory):
    environment = _marker_environment()
    installed = _installed_versions(library_directory)
    json_cache = {}
    selected = {}
    constraints = {}
    wheel_paths = {}
    pending = list(requirement_strings)
    total_wheel_size = 0

    while pending:
        raw_requirement = pending.pop(0)
        try:
            requirement = Requirement(raw_requirement)
        except Exception as error:
            raise ValueError("Invalid PEP 508 dependency {!r}: {}".format(raw_requirement, error))
        if requirement.url:
            raise ValueError("Direct URL dependencies are not supported: {}".format(raw_requirement))
        if requirement.extras:
            raise ValueError("Optional dependency extras are not supported: {}".format(raw_requirement))
        if not _requirement_applies(requirement, environment):
            continue

        name = canonicalize_name(requirement.name)
        constraints.setdefault(name, []).append(requirement.specifier)
        if name in selected:
            if any(not specifier.contains(selected[name]) for specifier in constraints[name]):
                raise RuntimeError("Conflicting requirements for {}".format(requirement.name))
            continue

        bundled = _bundled_version(requirement.name)
        existing = installed.get(name)
        available_version = existing or bundled
        if available_version is not None:
            if any(not specifier.contains(available_version) for specifier in constraints[name]):
                raise RuntimeError(
                    "Installed {} {} does not satisfy {}".format(
                        requirement.name, available_version, requirement.specifier
                    )
                )
            selected[name] = available_version
            # Bundled packages are part of the app build; packages installed by this manager
            # already have their transitive requirements in the shared libraries directory.
            continue

        version, filename, url, sha256 = _choose_wheel(name, constraints[name], json_cache)
        if name in selected and selected[name] != version:
            raise RuntimeError("Conflicting versions requested for {}".format(requirement.name))
        selected[name] = version
        wheel_path = os.path.join(temporary_directory, filename)
        total_wheel_size += _download_wheel(url, sha256, wheel_path)
        if total_wheel_size > MAX_TOTAL_WHEEL_SIZE:
            raise ValueError("Plugin dependencies exceed the 120 MB installation limit")
        wheel_paths[name] = wheel_path

        metadata = _read_wheel_metadata(wheel_path)
        if canonicalize_name(metadata.get("Name")) != name or Version(metadata.get("Version")) != version:
            raise ValueError("The downloaded wheel does not match PyPI metadata")
        requires_python = metadata.get("Requires-Python")
        if requires_python and not SpecifierSet(requires_python).contains(Version("{}.{}.{}".format(
                sys.version_info.major, sys.version_info.minor, sys.version_info.micro))):
            raise RuntimeError("{} {} requires Python {}".format(name, version, requires_python))
        for dependency in metadata.get_all("Requires-Dist", []):
            pending.append(dependency)

    return wheel_paths


def _wheel_install_path(filename):
    filename = filename.replace("\\", "/")
    if not filename or filename.startswith("/") or "\x00" in filename:
        raise ValueError("Unsafe path in Python wheel")
    normalized = posixpath.normpath(filename)
    if normalized == ".." or normalized.startswith("../"):
        raise ValueError("Unsafe path in Python wheel")
    if normalized.endswith(".pth") or normalized in ("sitecustomize.py", "usercustomize.py"):
        raise ValueError("Executable .pth and sitecustomize files are not accepted")
    if normalized.lower().endswith((".so", ".pyd", ".dll", ".dylib")):
        raise ValueError("Native extensions are not accepted in plugin dependencies")
    marker = ".data/purelib/"
    if marker in normalized:
        normalized = normalized.split(marker, 1)[1]
    elif ".data/" in normalized:
        raise ValueError("Wheel contains files outside its pure Python library")
    return normalized


def _install_wheels(wheel_paths, library_directory):
    os.makedirs(library_directory, exist_ok=True)
    staging = tempfile.mkdtemp(prefix="wheel-stage-", dir=library_directory)
    installed_paths = []
    total_unpacked = 0
    try:
        for wheel_path in wheel_paths.values():
            with zipfile.ZipFile(wheel_path) as wheel:
                for info in wheel.infolist():
                    if info.is_dir():
                        continue
                    name = _wheel_install_path(info.filename)
                    mode = (info.external_attr >> 16) & 0o170000
                    if mode == 0o120000:
                        raise ValueError("Symlinks are not accepted in Python dependency wheels")
                    total_unpacked += info.file_size
                    if total_unpacked > MAX_UNPACKED_SIZE:
                        raise ValueError("Unpacked plugin dependencies exceed the 120 MB limit")
                    destination = os.path.join(staging, *name.split("/"))
                    os.makedirs(os.path.dirname(destination), exist_ok=True)
                    with wheel.open(info) as source, open(destination, "wb") as target:
                        shutil.copyfileobj(source, target)

        for root, directories, files in os.walk(staging):
            relative_root = os.path.relpath(root, staging)
            target_root = library_directory if relative_root == "." else os.path.join(library_directory, relative_root)
            os.makedirs(target_root, exist_ok=True)
            for filename in files:
                staged_file = os.path.join(root, filename)
                target_file = os.path.join(target_root, filename)
                if os.path.exists(target_file):
                    with open(staged_file, "rb") as staged, open(target_file, "rb") as existing:
                        if staged.read() != existing.read():
                            raise RuntimeError("A dependency file conflicts with an installed package")
                    continue
                os.replace(staged_file, target_file)
                installed_paths.append(target_file)
    except BaseException:
        for path in reversed(installed_paths):
            try:
                os.unlink(path)
            except OSError:
                pass
        raise
    finally:
        shutil.rmtree(staging, ignore_errors=True)


def install_plugin_requirements(plugin_path, library_directory):
    requirements = _read_plugin_requirements(plugin_path)
    if not requirements:
        if library_directory not in sys.path:
            sys.path.insert(0, library_directory)
        return []

    os.makedirs(library_directory, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="irongram-wheels-") as temporary_directory:
        wheel_paths = _resolve(requirements, library_directory, temporary_directory)
        _install_wheels(wheel_paths, library_directory)

    if library_directory not in sys.path:
        sys.path.insert(0, library_directory)
    return sorted(wheel_paths.keys())

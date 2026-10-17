"""Compatibility models for Python plugin settings declarations.

Irongram renders the built-in text, switch, selector, input, and header controls. Custom
Android views and setting factories remain accepted as declarations but are not rendered.
"""


class Setting:
    def __init__(self, **values):
        self.__dict__.update(values)


class Header(Setting):
    pass


class Switch(Setting):
    pass


class Selector(Setting):
    pass


class Input(Setting):
    pass


class EditText(Setting):
    pass


class Divider(Setting):
    pass


class Text(Setting):
    pass


class Custom(Setting):
    pass


class SimpleSettingFactory:
    def __init__(self, create_view, bind_view, **options):
        self.create_view = create_view
        self.bind_view = bind_view
        self.options = options
        self.instance = self
        self.java = self

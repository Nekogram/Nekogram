if [ "$AGENT_JOBSTATUS" == "Succeeded" ]; then
    export name=$(find $APPCENTER_OUTPUT_DIRECTORY -name '*.apk')
    curl https://api.telegram.org/bot${BOT_TOKEN}/sendDocument -X POST -F chat_id="$CHANNEL_ID" -F document="@$name" -F caption="$(git log -1)"
fi
#!/bin/sh

/app/.profile.d/ngrok -proto=tcp -authtoken [your-ngrok-authtoken-here] -log=stdout 4000 > /app/.profile.d/ngrok.txt &
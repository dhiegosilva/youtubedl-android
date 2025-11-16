# OAuth Setup Instructions

This project uses OAuth 2.0 Device Authorization Grant flow (like SmartTube) for YouTube authentication.

## Setup

1. The OAuth credentials are stored in `app/src/main/assets/oauth.properties` (this file is gitignored and will NOT be committed).

2. If the file doesn't exist, copy the template:
   ```bash
   cp app/src/main/assets/oauth.properties.template app/src/main/assets/oauth.properties
   ```

3. Edit `app/src/main/assets/oauth.properties` and add your credentials:
   ```
   oauth.client.id=YOUR_CLIENT_ID_HERE
   oauth.client.secret=YOUR_CLIENT_SECRET_HERE
   ```

## Security

- ✅ `oauth.properties` is in `.gitignore` and will NOT be committed to git
- ✅ All credentials have been removed from source code
- ✅ The template file (`oauth.properties.template`) is safe to commit

## Current Credentials

The credentials are already configured in `app/src/main/assets/oauth.properties` for local development.

**IMPORTANT**: Never commit the `oauth.properties` file to version control!


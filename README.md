# Stash Http Request Trigger

After making commits to Stash, notify make a Http POST request to a specified URL, adding in query parameters for the Source Ref and Source Sha.  This plugin listens to both the mait repository and all pull-requests.


## Setup

Once installed, follow these steps:
-  Navigate to a repository in Stash.
-  Hit the *Settings* link
-  In the left-navigation, hit the *Hooks* link
-  For the **Stash Http Request Trigger**, click the *Enable* button.
-  Enter a URL

##Development
###Run
mvn -DdownloadSources=true eclipse:eclipse

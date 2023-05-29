#! /bin/bash

# Step.1 Download 8-ea of AOSP Projects
## Define constants
AOSP_DIR="./aosp"
AOSP_BASE_URL="https://android.googlesource.com/platform/packages/apps/"
PROJECTS=(
    "Calendar"      # Maintained until Android 12
    "Gallery"       # Maintained until Android 12
    "Messaging"     # Maintained until Android 12
    "Settings"      # Maintained until Android 12
    "Email"         # Maintained until Android 10
    "Calculator"    # Maintained until Android 7.1.2 
    "InCallUI"      # Maintained until Android 6
    "VideoEditor"   # Maintained until Android 4.4
)
TARGET_TAGS=(
    "android-12.0.0_r15"            # Latest tag for Calendar
    "android-12.0.0_r15"            # Latest tag for Gallery
    "android-12.0.0_r15"            # Latest tag for Messaging
    "android-12.0.0_r15"            # Latest tag for Settings
    "android-security-10.0.0_r60"   # Latest tag for Email
    "android-cts-7.1_r29"           # Latest tag for Calculator
    "android-cts-6.0_r32"           # Latest tag for InCallUI
    "android-cts-4.4_r4"            # Latest tag for VideoEditor
)

## Make AOSP_DIR directory for checkout
if ! test -d $AOSP_DIR; then 
    echo "Create AOSP directory:" $AOSP_DIR
    mkdir $AOSP_DIR
else
    echo "AOSP directory exist:" $AOSP_DIR
fi

## Checkout the target projects under AOSP_DIR
for ((index=0; index < "${#PROJECTS[@]}"; index++)); do
    project=${PROJECTS[$index]}
    tag=${TARGET_TAGS[$index]}
    git_url="$AOSP_BASE_URL$project" 
    checkout_dir="$AOSP_DIR/${project}_1"
    # Checkout
    if ! test -d $checkout_dir; then
        echo "Checkout: " $git_url
        git clone $git_url $checkout_dir
    else
        echo "Target directory exist:" $checkout_dir
    fi

    # Change current HEAD to target tag
    echo "Change to tag:" $tag
    ( cd $checkout_dir; git checkout $tag )
done

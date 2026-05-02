The purpose of the android app is to be able to quickly view images and mark specific ones for deletion while being able to pin an image and scroll through the rest of the images and compare against the pinned one.

# Motivation

There isn't a good photo culling app for Android that lets users quickly skim through images and decide which ones to keep and which ones to delete. The essence of the culling activity is to be able to pin an image and use it as a reference, and then scroll through rest of the images as a comparison against the pinned image. On a desktop, this is easy because the screen is big and a split screen of the pinned image and other images gives a good UX for comparisons. However, due to the small form factor of phones, a split screen would lead to a terrible UX - either the image has to be resized or cropped and the user would lose the point of culling and comparison.

# The App

Here's the idea for a photo culling app. At a high level, the MVP of the app lies in scrolling through images full screen, pinning an image and scrolling through rest of the images. The comparison will be done through touching and holding on the current image that would make the pinned image appear as long as the touch is held. Once the finger is let go, the current image is displayed.

## Features

- Home page must include two giant buttons - Library and Albums
- Selecting Library must load the images from the phone as thumbnails. The first time this is done, the folder where the images must be sourced from must be asked. This should open a folder picker. This should also be added in the Settings screen.
- Selecting Albums must be a no-op for now.
- Clicking on any thumbnail (assuming the user selecting Library in the home screen) should take the user to the photo that is opened full screen. The user must now be able to scroll through the photos in full screen. There must be a loading indicator that shows while the image takes time to load to full screen.
- There should be a small persistent bottom pill/button that brings up a HUD with a bottom bar that shows how many images are marked for deletion with a confirm button next to it. Hitting the confirm button must display a popup asking for confirmation where the popup says something like, "Would you like to delete N images" (where N is the number of images marked for deletion). The popup will have a Delete and a Cancel button. Pressing Delete will move those images to trash. The HUD should also show an icon to pin an image.
- Pinning an image from the HUD would show an indicator on the image that it's pinned. Show a Pin icon on the top left corner of the image. Only one image can be pinned at any time. If there is already a pinned image and a different image is selecting for pinning, the pin should simply move to that image.
- Pin status of an image is stored so that as the user scrolls through the images, if they scroll back and forth, they'd know it's a pinned image if they land on one
- Long pressing on any image should reveal the pinned image for as long as the finger is held. Once the finger is let go, the current image must be displayed. Long pressing on the pinned image should do nothing, but instead show a toast that says "Cannot compare a pinned image against itself". If the HUD is shown by tapping on an image, long pressing should remove the HUD and show the pinned image. If there's any system-level long press behavior the conflicts with this, it must be suppressed.
- Long press threshold (in milliseconds) must be configurable in the settings
- Pinch to zoom must be supported. If the user zooms in on any part of the image, the pinned image must also be zoomed in accordingly. That way, when they long press after zooming in, the pinned image would have also zoomed in accordingly. This helps the user compare the same part of two different images. The same applies to panning after zooming in. If the user pans around the current image, the pinned image must also be moved accordingly.
- When the user zooms in, a new button must appear in the top right corner of the image called Reset Both Zooms, clicking on it should reset the zoom level of both the current image and the pinned image. This button must be translucent. It must disappear once the zoom is reset and reappear when zoom is applied.
- Swipe down on an image to mark the image for deletion. The image marked for deletion must be indicated by making the image greyscale and displaying a text in the top right corner of the image saying "Marked for deletion" with a small undo button next to it. Pressing the undo button undoes this change. The swiping down action must animate the image to move down a little. Swiping down on an image already marked for deletion would unmark the image and remove the greyscale.
- The app must support both local and self hosted instance for loading images, specifically Immich.
- Settings screen must have inputs to configure the Immich URL and the API key. It must also have a Test Connection button that tests the connection to the server.
- The Settings screen must have a dry run mode checkbox. Checking this would disable any actual deletions. When the user hits confirm on photos marked for deletion, there will be a Toast message that says "Dry run mode enabled. No photos were deleted".
- On every image, on the HUD, there should be a cloud icon. It must have a slash through it if that image is not found on Immich. Else, it must be a regular cloud icon.
- If the photos marked for deletion contain photos that also exist on the Immich instance, display a message in the confirmation dialog that tells the user how many photos are on local, and how many are on both local and Immich.
- If the photos marked for deletion also exist on Immich, then Immich's delete API must be called. The same dry run feature applies to deleting Immich photos as well.
- THIS IS IMPORTANT: Actual deletions must involve moving photos to Trash, not actually deleting the photos from the phone.
- If the user is in the middle of a culling session and they exit the app, store the last viewed position (the image they were on) in a persistent storage. Upon reopening the app, show a button called "Continue where you left off". Clicking that button should take the user to the image they were on before they closed the app.


<img src="art/scissors.png" width="256" align="right" hspace="20" />

Scissors
=========================

Fixed viewport image cropping library for Android with built-in support for [Picasso][picasso] or [Glide][glide].

Usage
-----

See `scissors-sample`.

<img src="art/demo.gif" width="320" align="right" hspace="20" />


- Include it on your layout:
```xml
<com.lyft.android.scissors.CropView
    android:id="@+id/crop_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:cropviewViewportHeightRatio="1"
    />
```
-  Set a Bitmap to be cropped. In example by calling `cropView.setImageBitmap(someBitmap);`
-  Call `Bitmap croppedBitmap = cropView.crop();` to obtain a cropped Bitmap to match viewport dimensions

Extensions
----------
Scissors comes with handy extensions which help with common tasks like:

#### Loading a Bitmap
To load a Bitmap automatically with [Picasso][picasso] or [Glide][glide] into `CropView` use as follows:

```java
cropView.extensions()
    .load(galleryUri);
```
#### Cropping into a File
To save a cropped Bitmap into a `File` use as follows:

```java
cropView.extensions()
    .crop()
    .quality(87)
    .format(PNG)
    .into(croppedFile))
```

Questions
----------
For questions please use github issues. Mark question issue with "question" label.

Download
--------

```groovy
compile 'com.lyft:scissors:1.0.1'
```

Snapshots of development version are available in [Sonatype's `snapshots` repository][snap].

License
-------

    Copyright (C) 2015 Lyft, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

Contributing
------------

Please see `CONTRIBUTING.md`.

Contributors
------------
- [Evelio Tarazona] (https://github.com/eveliotc)
- [Helen Lau](https://github.com/hlau-lyft)
- [Alexey Zakharov](https://github.com/lexer)
- [Thomas Ezan](https://github.com/thomaslyft)
- [Join us, work for Lyft](https://www.lyft.com/jobs)

 [snap]: https://oss.sonatype.org/content/repositories/snapshots/
 [picasso]: https://github.com/square/picasso
 [glide]: https://github.com/bumptech/glide

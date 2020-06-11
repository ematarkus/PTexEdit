# PTexEdit
PTexEdit is a graphical Java application allowing for read and write operations on papa files which store textures.

"Open" and "save" are used to read and write papa files, "Import" and "Export" are used to read and write image files.
Some textures are stored as references to other textures. PTexEdit is capable of reading these references and loading the files, but you must set the "media" directory which can be found at <PA's root directory> / media. You can set this directory to anything you want in the case that you are loading modded textures.

Currently all file formats that PA uses are fully read and write supported. DXT compression is implemented through the use of JSquish (https://github.com/memo33/jsquish), however it is slower than papatran and produces slightly lower quality results. If you need the absolute best results it's recommended to use papatran for DXT.

Reading and writing a file will not alter the actual image data, so it is safe to use PTexEdit to just edit the name or SRGB status of a texture. However, files containing non image data will have that data erased when saving as the program only supports images.

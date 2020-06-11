# PTexEdit
PTexEdit is a graphical Java application allowing for read and write operations on papa files which store textures.

### Usage
PTexEdit supports drag and drop operations for loading files and folders. If the program can be certain of what operation you want to do it will automatically start processing the input, otherwise it will prompt on whether to open in papa or image mode. "Open" and "Save" are used to read and write papa files, "Import" and "Export" are used to read and write image files. Only drag and drop properly supports folder reading.

Some textures are stored as references to other files. PTexEdit is capable of reading these references and loading the files, but you must set the "media" directory which can be found at <PA's root directory> / media. You can set this directory to anything you want in the case that you are loading modded textures.

### Additional Info
Currently all file formats that PA uses (R8G8B8A8, R8G8B8X8, B8G8R8A8, DXT1, DXT3, DXT5, and R8) are fully read and write supported. DXT compression is implemented through the use of JSquish (https://github.com/memo33/jsquish), however it is slower than papatran and produces slightly lower quality results. If you need the absolute best results it's recommended to use papatran for DXT.

Reading and writing a file will not alter the actual image data, so it is safe to use PTexEdit to just edit the name or SRGB status of a texture. However, files containing non image data will have that data erased when saving as the program only supports images.

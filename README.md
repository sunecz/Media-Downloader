# Media-Downloader
Simple application for downloading media from various websites.

## How to build
Building can be done either manually, or using Apache's Ant (recommended).
The Ant script can be run either directly on a host machine or in the prepared Docker image.

### Using Ant
Simply run `ant` in the root directory of this repository. Furthermore, any of these targets may be specified:
- `compile`: Copies all resources from the `src` directory and compiles all Java files to the `bin` directory.
- `build`: Builds `media-downloader.jar` and `media-downloader-source.jar` files in the `build` directory.
- `dist-windows`: Builds distribution archive for the Windows operating system in the `dist` directory.
- `dist-linux`: Builds distribution archive for the Linux operating system in the `dist` directory.
- `dist-osx`: Builds distribution archive for the Mac OS X operating system in the `dist` directory.
- `dist-all`: Builds distribution archives for Windows, Linux and Mac OS X operating systems in the `dist` directory.
- `clean`: Removes the `bin` and `build` directories.

The default target is `build`. This target is run when no target is specified, i.e. by just running `ant`.
To specify a target, run `ant TARGET`, where `TARGET` is any of the targets above.

**Note**: Targets `dist-windows`, `dist-linux`, `dist-osx` and `dist-all` all rely on script
`etc/scripts/dist.sh` which can be run only on systems that have `/bin/bash` present.

### Using the Docker image
First, the Docker image needs to be build. To build the image, run the following command
in the root directory of this repository:
```
docker build --no-cache -t md:build -f docker/Dockerfile
```

To run the `ant` command in the Docker image, run the following command:
- For Windows operating system:
```
docker run --rm -v "%cd%:/workdir" --name md-build -it md:build ant
```
- For Linux/Mac OS X operating systems:
```
docker run --rm -v "$(pwd):/workdir" --name md-build -it md:build ant
```

The `ant` at the end of the command can be replaced by `ant TARGET`, where `TARGET` is any of the targets above.

To remove the built Docker image, run the following command:
```
docker image rm md:build
```

# Related repositories
- Default plugins: https://github.com/sunecz/Media-Downloader-Default-Plugins
- DRM plugin: https://github.com/sunecz/Media-Downloader-DRM-Plugin
- Launcher: https://github.com/sunecz/Media-Downloader-Launcher

# Media-Downloader
Simple application for downloading media from various websites.

## How to build
Building can be done either manually, or using [Apache's Ant](https://ant.apache.org/).
The Ant script can be run either directly on the host machine or in the prepared Docker image.

To run the following commands on Windows, use PowerShell.

### Clone the repository
```shell
git clone https://github.com/sunecz/Media-Downloader.git
cd Media-Downloader/
```

### Build using the Docker image
First, build the Docker image:
```shell
docker build --no-cache -t md:build -f docker/Dockerfile docker/
```

Download the [JavaFX platform (OpenJFX)](https://openjfx.io/) SDK:
```shell
curl -o docker/openjfx.zip JAVAFX_URL
mkdir -p docker/openjfx
tar -xf docker/openjfx.zip --directory docker/openjfx --strip-components=1
```
where `JAVAFX_URL` is one of these, depending on the operating system:
| Operating system | JAVAFX_URL                                                                          |
| ---------------- | ----------------------------------------------------------------------------------- |
| Windows          | https://download2.gluonhq.com/openjfx/11.0.2/openjfx-11.0.2_windows-x64_bin-sdk.zip |
| Linux            | https://download2.gluonhq.com/openjfx/11.0.2/openjfx-11.0.2_linux-x64_bin-sdk.zip   |
| Mac OS X         | https://download2.gluonhq.com/openjfx/11.0.2/openjfx-11.0.2_osx-x64_bin-sdk.zip     |

Finally, build the JAR:
```shell
docker run --rm -v "$(pwd):/workdir" --name md-build -it md:build ant -D"path.javafx"="docker/openjfx" build
```

### Clean up

To remove the built Docker image:
```
docker image rm md:build
```

### Ant targets
| Target name  | Description                                                                                             |
| ------------ | ------------------------------------------------------------------------------------------------------- |
| compile      | Copies all resources from the `src` directory and compiles all Java files to the `bin` directory.       |
| build        | Builds `media-downloader.jar` and `media-downloader-source.jar` files in the `build` directory.         |
| dist-windows | Builds distribution archive for the Windows operating system in the `dist` directory.                   |
| dist-linux   | Builds distribution archive for the Linux operating system in the `dist` directory.                     |
| dist-osx     | Builds distribution archive for the Mac OS X operating system in the `dist` directory.                  |
| dist-all     | Builds distribution archives for Windows, Linux and Mac OS X operating systems in the `dist` directory. |
| clean        | Removes the `bin` and `build` directories.                                                              |

The default target is `build`. This target is run when no target is specified, i.e. by just running `ant`.
To specify a target, run `ant TARGET`, where `TARGET` is any of the target names above.

**Note**: Targets `dist-windows`, `dist-linux`, `dist-osx` and `dist-all` all rely on script
`etc/scripts/dist.sh` which can be run only on systems that have `/bin/bash` present.

# Related repositories
- Default plugins: https://github.com/sunecz/Media-Downloader-Default-Plugins
- DRM plugin: https://github.com/sunecz/Media-Downloader-DRM-Plugin
- Launcher: https://github.com/sunecz/Media-Downloader-Launcher

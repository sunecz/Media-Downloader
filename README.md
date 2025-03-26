# Media Downloader
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

Then build the JAR:
```shell
docker run --rm -v "$(pwd):/workdir" --name md-build -it md:build ant build
```

### Clean up

To remove the built Docker image:
```shell
docker image rm md:build
```

## Ant targets
| Target name | Description                                                                                       |
| ----------- | ------------------------------------------------------------------------------------------------- |
| compile     | Copies all resources from the `src` directory and compiles all Java files to the `bin` directory. |
| build       | Builds `media-downloader.jar` and `media-downloader-source.jar` files in the `build` directory.   |
| clean       | Removes the `bin` and `build` directories.                                                        |

The default target is `build`. This target is run when no target is specified, i.e. by just running `ant`.
To specify a target, run `ant TARGET`, where `TARGET` is any of the target names above.

# Related repositories
- Default plugins: https://github.com/sunecz/Media-Downloader-Default-Plugins
- DRM plugin: https://github.com/sunecz/Media-Downloader-DRM-Plugin
- Launcher: https://github.com/sunecz/Media-Downloader-Launcher

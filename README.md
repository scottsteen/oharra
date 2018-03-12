d# Oharra

[Oharra](https://translate.google.com/?sl=eu#en/eu/release) is a no-frills release plugin for Gradle projects.

The plan is simple - keep the plugin simple! Oharra will not try to dictate how you build or deploy your project, 
it simply provides a way to track the releases of your project, using the SCM of your choice.

## Usage

Add the version of oharra that you wish to use to your buildscript, and invoke `gradle ${yourBuildAndDeployTasks} release`. That's it!  
Oharra, by default, will use the current version (minus the `-SNAPSHOT` if it exists) as the current release version,
and increment the patch version by one and append `-SNAPSHOT` for the next development version.

See [the example](./example/build.gradle) for further information.

### Configuration

You can specify the release version to use by passing the `releaseVersion` property. 
Similarly, you can pass `developmentVersion` to specify the next development version.

Further configuration is provided using the oharra extension. The following is all configuration options, and their defaults.
```groovy
oharra {
    versionFile "gradle.properties"
    releaseMessage = "[Gradle Oharra plugin] Committing release version"
    postReleaseMessage = "[Gradle Oharra plugin] Preparing for new development"
    developmentVersionSuffix "-SNAPSHOT"
    testMode { // disables scm interaction, for testing only!
        printOutput true
    }
    // testMode() is also allowed, if no configuration is required
    
    git {
        remote "origin"
    }
}
```

## Multi-project builds

Due to its simple nature, oharra fully supports multi-project builds. As long as this plugin is at least applied 
to the root project, it'll just work. If you apply this plugin to every project, the release is still only performed 
once!

## Limitations

Oharra assumes your project uses [semantic versioning](https://semver.org/).  
Currently, git is the only supported SCM.
The 'git' executable must be available on the PATH.
Rollbacks are not automatically performed if an error occurs.  
The release version cannot equal the current version.  
_All_ local tags will be transferred to the git server.  

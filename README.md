# Wurst Client v7 Enhanced

![Wurst Client logo](/src/main/resources/assets/wurst/wurst_128.png)

The client version is designed for playing Skywars/Bedwars. But of course, you can use this client for more than just that.. 

- **Download:** [https://github.com/breelock/Wurst7/releases](https://github.com/breelock/Wurst7/releases)

- **Installation guide:** [https://www.wurstclient.net/tutorials/how-to-install/wurst-7/](https://www.wurstclient.net/tutorials/how-to-install/wurst-7/)

- **Original feature list:** [https://www.wurstclient.net/](https://go.wimods.net/from/github.com/Wurst-Imperium/Wurst7?to=https%3A%2F%2Fwww.wurstclient.net%2F%3Futm_source%3DGitHub%26utm_medium%3DWurst7%2Brepo)

- **Original client**: [https://github.com/Wurst-Imperium/Wurst7](https://github.com/Wurst-Imperium/Wurst7)

## Difference from the original version

```
- Normal .cfg system (All settings just in one .json file)
- AspectRatio
- ClickPearl
- AutoFriends (for you minigames teammates)
- AutoYLeave
- AutoToolDrop
- RadarHack only players mode
- AutoArmor drop worst armor
- AutoDrop disable after drop
- Anti anti-cheat flag system on AutoArmor, AutoSword, AutoDrop, etc.
- Now you can use mouse binds
- Enhanced SafeWalk, now you can control the permitted height
- Enhanced AutoSteal
- Enhanced PlayerEsp, ChestEsp, ItemEsp, etc.
- Enhanced GUI, custom GUI color, custom wurst logo
- Discord RPC
- Deleted some trash
- Slightly improved translation
- Some fixes and improvements
```

## Installation

Wurst 7 can be installed just like any other Fabric mod. Here are the basic installation steps:

1. Run the Fabric installer.
2. Add the Wurst Client and Fabric API to your mods folder.

Please refer to the [full Wurst 7 installation guide](https://go.wimods.net/from/github.com/Wurst-Imperium/Wurst7?to=https%3A%2F%2Fwww.wurstclient.net%2Ftutorials%2Fhow-to-install%2F%3Futm_source%3DGitHub%26utm_medium%3DWurst7%2Brepo) if you need more detailed instructions or run into any problems.

## Development Setup

> [!IMPORTANT]
> Make sure you have [Java Development Kit 17](https://adoptium.net/?variant=openjdk17&jvmVariant=hotspot) installed. It won't work with other versions.

1. Clone the repository:

   ```pwsh
   git clone https://github.com/Wurst-Imperium/Wurst7.git
   cd Wurst7
   ```

### Development using IntelliJ IDEA

2. Generate the sources:

   ```pwsh
   ./gradlew genSources idea
   ```

### Development using VSCode / Cursor

> [!TIP]
> You'll probably want to install the [Extension Pack for Java](https://go.wimods.net/from/github.com/Wurst-Imperium/Wurst7?to=https%3A%2F%2Fmarketplace.visualstudio.com%2Fitems%3FitemName%3Dvscjava.vscode-java-pack) to make development easier.
>
2. Generate the sources:

   ```pwsh
   ./gradlew genSources vscode
   ```

3. Open the `Wurst7` folder in VSCode / Cursor.

4. **Optional:** In the VSCode settings, set `java.format.settings.url` to `https://raw.githubusercontent.com/Wurst-Imperium/Wurst7/master/codestyle/formatter.xml` and `java.format.settings.profile` to `Wurst-Imperium`.

### Development using Eclipse

2. Generate the sources:

   ```pwsh
   ./gradlew genSources eclipse
   ```

3. In Eclipse, go to `Import...` > `Existing Projects into Workspace` and select this project.

4. **Optional:** Right-click on the project and select `Properties` > `Java Code Style`. Then under `Clean Up`, `Code Templates`, `Formatter`, import the respective files in the `codestyle` folder.

## License

This code is licensed under the GNU General Public License v3. **You can only use this code in open-source clients that you release under the same license! Using it in closed-source/proprietary clients is not allowed!**

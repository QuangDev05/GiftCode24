# GiftCodePlugin

GiftCodePlugin is a Minecraft plugin that allows you to manage and use gift codes on your server. This plugin provides commands for admins to create, enable/disable, delete, and reload gift codes, as well as commands for players to redeem codes and receive rewards. All configurations related to gift codes, messages, usage limits, and expiration times can be easily adjusted in the `config.yml` file.

## Features

- Manage gift codes through commands or configuration files.
- Limit the number of times each player can use a code.
- Create gift codes with customizable reward commands.
- Clear notifications for players when entering codes.
- Easily enable/disable gift codes.
- Reload configuration without restarting the server.
- Delete unused gift codes.

## Installation

1. Download the .jar file of the plugin.
2. Place the .jar file into the `plugins` folder of your Minecraft server.
3. Restart the server to load the plugin.
4. Edit the `config.yml` file to suit your needs.

## Configuration

Below is an example configuration in the `config.yml` file:

```yaml
# List of gift codes and their information
codes:
  samplecode:
    commands: 
      - give %player% diamond 1
    message: "You have received 1 diamond!"
    max-uses: 10
    expiry: 2024-12-31T23:59:59
    enabled: true

# Various messages
messages:
  invalid-code: "The gift code you entered is invalid."
  expired-code: "The gift code you entered has expired."
  max-uses-reached: "The gift code you entered has reached its maximum number of uses."
  code-disabled: "The gift code you entered is currently disabled."
  code-redeemed: "You have successfully redeemed the gift code!"
  code-already-redeemed: "You have already redeemed this code."

# Limit the number of times each player can use a code
player-max-uses: 1
```

## Commands

- `/giftcode create <code>`: Create a new gift code.
- `/giftcode del <code>`: Delete a gift code.
- `/giftcode reload`: Reload the configuration file.
- `/giftcode enable <code>`: Enable a gift code.
- `/giftcode disable <code>`: Disable a gift code.
- `/giftcode help`: Display a list of commands and plugin information.
- `/code <code>`: Redeem a gift code and receive rewards.

## Permissions

- `giftcode.admin`: Allows the use of gift code management commands.
- `giftcode.player`: Allows players to redeem gift codes.

## Author

QuangDev05

## Version

1.0

## Contributing

All contributions are welcome! If you have any ideas or want to report a bug, please open an issue or pull request on GitHub.

## License

This plugin is released under the MIT License. See the LICENSE file for more details.
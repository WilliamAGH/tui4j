# Showcases

This directory contains high-fidelity replicas of popular TUI applications to demonstrate the capabilities of TUI4J.

## Pulse UI Replica

`PulseExample.java` demonstrates a polished, modern TUI inspired by the [Crush](https://github.com/charmbracelet/crush) AI coding assistant.

### Features
- **Tabs**: Navigation between different views (Code, Diff, Plan, Chat).
- **Split Layout**: Sidebar for file navigation and main content area.
- **Responsive Design**: Adapts to window resize events.
- **Animations**: Integrated spinner animation for "thinking" states.
- **Lipgloss Styling**: Custom colors and borders to resemble the "Crush" aesthetic.

### Running

To run this example, you need to compile it with the project dependencies (packaged in `tui4j-examples.jar`).

1. **Build the examples jar:**
   ```bash
   ./gradlew examplesJar
   ```

2. **Compile and Run:**
   ```bash
   # Create a temporary directory for classes
   mkdir -p build/tmp_classes

   # Compile the showcase
   javac -cp build/libs/tui4j-examples.jar -d build/tmp_classes src/main/resources/examples/showcases/PulseExample.java

   # Run it
   java -cp "build/libs/tui4j-examples.jar:build/tmp_classes" examples.showcases.PulseExample
   ```

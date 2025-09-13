# Daily Tasks Plugin

The **Daily Tasks Plugin** is an automation tool for Old School RuneScape, designed to help players efficiently complete their daily in-game activities. Built for the Microbot RuneLite client, this plugin automates the process of tracking, managing, and completing daily tasks, ensuring you never miss out on valuable rewards and experience.

---

## Features

- **Automated Daily Task Completion:**  
  Automatically performs a variety of daily activities, such as collecting resources, visiting NPCs, or completing routine actions.

- **Task Tracking and Management:**  
  Keeps track of which daily tasks have been completed and which are still pending, helping you stay organized.

- **Overlay Display:**  
  Real-time overlay shows current status, tasks completed, tasks remaining, and other useful stats.

- **Configurable Options:**  
  Users can select which daily tasks to automate, enable/disable the overlay, and adjust advanced behaviors in the configuration panel.

- **Failsafes and Error Handling:**  
  Handles running out of required items, full inventory, or unexpected in-game events.

---

## How It Works

1. **Configuration:**  
   Select which daily tasks you want to automate in the plugin panel.

2. **Startup:**  
   The plugin checks your progress and inventory, preparing for the selected daily tasks.

3. **Automation Loop:**  
   The script performs the selected daily tasks, manages inventory, and tracks completion status.

4. **Overlay:**  
   Displays real-time information such as:
    - Current action (e.g., "Collecting", "Visiting NPC", "Banking")
    - Tasks completed and remaining
    - Runtime and efficiency stats

5. **Failsafes:**  
   Pauses or stops if requirements are not met, or if unexpected events occur.

---

## Configuration

The plugin provides a configuration panel (`DailyTasksConfig`) where you can:

- Select which daily tasks to automate
- Enable or disable the overlay
- Adjust advanced options (delays, anti-patterns, etc.)

---

## Requirements

- Microbot RuneLite client
- Required items or access for specific daily tasks

---

## Usage

1. **Enable the Plugin:**  
   Open the Microbot sidebar, find the Daily Tasks Plugin, and enable it.

2. **Configure Settings:**  
   Select your desired daily tasks and adjust settings as needed.

3. **Start the Plugin:**  
   Click "Start" to begin automated daily task completion.

4. **Monitor Progress:**  
   Watch the overlay for real-time updates on progress and status.

5. **Stop at Any Time:**  
   Click "Stop" to halt the automation.

---

## Limitations

- Only supports daily tasks defined in the script logic.
- Requires the player to have the necessary items or access for certain tasks.
- May not handle all random events or interruptions (e.g., player death, aggressive NPCs).

---

## Source Files

- `DailyTasksPlugin.java` – Main plugin class, manages lifecycle and integration.
- `DailyTasksScript.java` – Core automation logic for daily tasks.
- `DailyTasksConfig.java` – User configuration options.
- `DailyTasksOverlay.java` – In-game overlay display.
- `DailyTask.java` – Task definitions and logic.

---

**Automate your daily RuneScape activities and never miss a reward with the Daily Tasks Plugin!**
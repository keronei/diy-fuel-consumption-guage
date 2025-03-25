# Fuel Consumption Gauge

### How it works
 - Utilizes MPGuino which takes in fuel injector pulses, VSS pulses(vehicle speed sensor) and an extra one from 
fuel tank to approximate the amount of remaining fuel.
 - The info is aggregated to give a realtime speed, RPM, fuel consumption, etc which is displayed on a 16x2 LCD
attached to an Arduino.
 - The data is also sent via serial at 4 updates per second which is received and displayed by the Android device.

### Screenshot from Android Display
<img src="/images/guage.png" width="960" height="540" alt="Screenshot from Android with guage"/> &npsb; <img src="/images/linear.png" width="960" height="540" alt="Screenshot from Android with linear RPM and detailed trip info"/>

### What currently works
- [-] Instant KM/L consumption
- [-] RPM
- [-] Speed
- [-] Trip aggregates(avg speed, trip time, distance)
- [] Fuel tank balance(used for remaining distance approximation)
- [] Consumption graph 

### Circuitry
<img src="/images/t2_schematic_v16.jpg" width="700" height="453" alt="The circuit"/>
This is the original template from MPGuino project, which is the [http://mpguino.wiseman.ee/eng/mpguino](Wiseman's variant). 

<img src="/images/level_sender.png" width="1024" height="592" alt="The circuit"/>
The fuel tank reader, my tank is very wide and makes it hard to guage the amount remaining, taken from [https://www.caterhamlotus7.club/forums/topic/272927-fuel-level-sender-how-to-convert-resistance-to-a-0-5v-signal/](Caterham & Lotus 7 Club)


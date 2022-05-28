# ⚡2022 Purdue Fire Evacuation System by Bacon Beacon⚡
<hr>

📑 *Project Title*
        
    Beacon-based Evacuation System and Technology

📅 *Project Period*

    04-17-2022(SUN) ~ 08-05-2022(FRI)

🧖🏻‍♀️ *Problem Statement*
    
    There are many people injured due to the fire. However, current evacuation system is not good enough to evacuate many people due to the smoke.

    There were several attempts to try to fix this issue, However, they are not good enough. Wi-Fi based localization and Pedestrian Dead Reckoning has its limits on accuracy, and 2-dimentional Navigation is not good enough to make people evacuate efficiently.


📖 *Considerations*

    🥕Software : Develop an algorithm that can evacuate people efficiently.
    
    🥕Hardware : Using Beacons which consumes less energy to locate it every floors

💡 *Novelty*

    1. High accuracy of indoor localization using iBeacon
       => We researched about Wi-Fi based indoor localization, and we figured out that it is not accurate enough due to lack of Access Point.
      We include beacon and Access Point when we localize, and we will gain 22% increase in accuracy.
      
    2. Real-Time socket communication and Optimized Route Algorithm
       => Most previous research works on Evacuation Algorithm based on database. It means that they cannot modify the evacuation route based on real-time data.
      Our team concentrates on real-time route modification to make sure that victims can stay away from hazards
      Our project also detects location of fire, so people may know the location of fire.
    3. Augmented Reality
      Since we developed optimized route algorithm to calculate the best route, if we cannot give advice to victims clearly, it would be useless.
      Previous work used 2D navigation and image-based navigation. for 2D navigation, It has its limits on being too dependent on angles, that users cannot notice if they are on the right route.
      Image based navigation, has its limits on malfunctioning at real situation full of smoke.
      We developed Augmented Reality for those who cannot notice exit sign. It is clear to see, and it works when they does not have clear sight.

🏛 *System Overview*
 <p align="center">
   <img src="https://github.com/BeaconAR/BEST/raw/main/image/Overview.png" alt="Image Error"/>
</p>
    
    1. Raspberry Pi always tries to detect fire, and server always checks if Raspberry Pi works or not. Once Raspberry detects fire, It sends to server HTTP request to let the server know the situation of fire.
    
    2. Server gets blueprint of building from Database, and sends the beacon status if it works well.
    
    3. Server sends notification to victim's mobile device, and 
    
    4. If smartphone opens the application, it start collecting Beacon's RSSI, UUID, major and minor value.
    
    5. Smartphone start calculating current location from the data collected via machine learning, and sends server location data
    
    6. Server calculates the fastest way to escape, and sends back to smartphone. At this part, A* or Dijkstra Algorithm will be used
 
🖥️ *Environment Setting*

    ✔️Raspberry Pi 3
    
    ✔️Arduino IDE version 1.8.13
    
    ✔️Python version 3.7.3 
    
    ✔️Swift 5.5
    
    ✔️iPhone 12 with iOS 15.4
    
    ✔️Kotlin

    ✔️Android Studio
  
👨‍👩‍👧‍👧 *Collaborator*
     
    👩‍💻Hwawon Lee
       -Soongsil University
       -Major in School of Software, Cyber Security
       -andylhw12@soongsil.ac.kr
       -https://github.com/andylhw
       
    🎅🏻Dohyun Chung
       -Chungang University
       -Major in Industrial Security
       -sosvast@cau.ac.kr
       -https://github.com/pastapeter
      
    👰Yoonha Bahng
       -Chungang University
       -Major in Computer Science Engineering
       -tlol91@cau.ac.kr
       
       
    👩‍🚀Jiwon Lim
       -Kwangwoon University
       -Major in Computer Science
       -senta2006@kw.ac.kr
       
    
    👨🏻‍🦱Suhyun Park
       -Paicai University
       -Major in Computer Science
       -2061013@pcu.ac.kr

    
    👨🏻‍💼Seongmin Kim
       -Kangwon National University
       -Major in Computer Science
       -aliveksm@kangwon.ac.kr
    
    🧔🏻Myoung Oh
       -Purdue University
       -Major in CNIT
       -oh278@purdue.edu

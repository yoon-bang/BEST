# ⚡2022 Purdue Fire Evacuation System by Bacon Beacon⚡
<hr>

📑 *Project Title*
        
    Beacon-based Evacuation System and Technology

📅 *Project Period*

    04-17-2022(SUN) ~ 08-05-2022(FRI)

🧖🏻‍♀️ *Problem Statement*
    
    In United States there are 1009 fires every day and 42 residential fires every hour[1].

    Due to the limitation of current evacuation plan using EXIT sign, evacuees will have a difficult time evacuating if the buildings are complex and individuals       
    struggle to understand the building’s design.

    There were several attempts to try to fix this issue, However, they are not good enough. Wi-Fi based localization and 
    Pedestrian Dead Reckoning has its limits on the accuracy, and 2-dimensional Navigation is not good enough to make people 
    evacuate efficiently.

    Therefore, a system that can both provide high accuracy localization and efficiently way to evacuating people is needed.

    [1] U.S. Fire Administration, FEMA[2021 online]. Available: Fire Estimate Summary


📖 *Considerations*

    🥕Software : Develop an algorithm that can evacuate people efficiently.
    
    🥕Hardware : Using Beacons which consumes less energy to locate every floor.

💡 *Novelty*

    1. High accuracy of indoor localization using iBeacon
       => We researched Wi-Fi based indoor localization, and we figured out that it is not accurate enough due 
       to lack of Access Points.
       We include beacon and Access Point when we localize, and we will gain a 22% increase in accuracy.
      
    2. Real-Time socket communication and Optimized Route Algorithm
       => Most previous research works on Evacuation Algorithms based on the database. It means that they cannot modify 
       the evacuation route based on real-time data.
       Our team concentrates on real-time route modification to make sure that victims can stay away from hazards
       Our project also detects the location of the fire, so people may know the location of the fire.
      
    3. Augmented Reality
       => Since we developed an optimized route algorithm to calculate the best route, if we cannot advise victims 
       it would be useless.
       Previous work used 2D navigation and image-based navigation. for 2D navigation, It has its limits on being too 
       dependent on angles, that users cannot notice if they are on the right route.
       Image-based navigation has its limits on malfunctioning in a real situation full of smoke.
       We developed Augmented Reality for those who cannot notice exit sign. It is clear to see, and it works when 
       they do not have clear sight.

🏛 *System Overview*
 <p align="center">
   <img src="https://github.com/BeaconAR/BEST/raw/main/image/Overview.png" alt="Image Error"/>
</p>
    
    1. Raspberry Pi always tries to detect fire, and the server always checks if Raspberry Pi works or not. Once 
    Raspberry Pi detects a fire, It sends to a server an HTTP request to let the server know the situation of fire.
    
    2. Server gets blueprint of building from Database and sends the beacon status if it works well.
    
    3. Server sends a notification to the victim's mobile device, and 
    
    4. If the smartphone opens the application, it starts collecting Beacon's RSSI, UUID, major and minor value.
    
    5. Smartphone starts calculating the current location from the data collected via machine learning and sends 
    server location data
    
    6. Server calculates the fastest way to escape and sends it back to the smartphone. At this part, A* or 
    Dijkstra Algorithm will be used
    
 <p align="center">
   <img src="https://github.com/BeaconAR/BEST/raw/main/image/topology.png" alt="Image Error"/>
</p>
    
    Localize each user’s position : When raspberry pi detects fire with sensor, it send HTTP request to our server. As ther server with connected with a user’s
    smartphone with TCP connection, it makes the smartphone to receive beacon’s signal. After receiving the signal, it calculates RSSI values and localize the user’s 
    position by using triangulation.

    Optimal Evacuation Algorithm : The smartphone sends the calculated position to the server through TCP connection. The position can be synchronized in real-time 
    due to TCP connection. The server configures an optimal evacuation route with the received position and our algorithm. If any change occurs, server will find a 
    new evacuation route and notify it to the smartphone.


 
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
       -Paichai University
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

# Beacon-based Indoor Fire Evacuation System using Augmented Reality and Machine Learning

![Generic badge](https://img.shields.io/badge/Xcode-13.3.1-blue.svg)  ![Generic badge](https://img.shields.io/badge/iOS-13.0-yellow.svg)  ![Generic badge](https://img.shields.io/badge/Swift-5.5-green.svg)  ![Generic badge](https://img.shields.io/badge/Firebase-ios-sdk.svg)  ![Generic badge](https://img.shields.io/badge/Socket.IO-15.0-purple.svg)   ![Generic badge](https://img.shields.io/badge/TensorflowliteSwift-2.9.1-orange.svg)[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

BEST is the indoor Fire Evacuation System. The name BEST is short for Beacon-based Indoor Fire Evacuation System using Augmented Reality and Machine Learning. This system would be used for the multi-floor building as an function for evacuating people from the fire situations.

<p class='center'>
    <img src="https://user-images.githubusercontent.com/69891604/184859167-452ede1a-c84c-4631-9c0c-c539267ab04f.PNG" width="200" height="400"/>
    <img src="https://user-images.githubusercontent.com/69891604/184859150-8ef583b9-0fb2-4395-9435-5cd54b76807b.PNG" width="200" height="400"/>
</p>


Best provides:
* **Indoor Fire Evacuation system across iOS and Android.** BEST can support iOS and Android, enabling for evacuate people regardelss of what devices they use. 
* **High Accuracy of Indoor localization** BEST use the multi-classification with DNN. The accuracy of the Model is 78%. Also use the algorithm for adjust indoor localization.
* **2D map with cell status and 3D AR navigation system.** BEST display 2D map and 3D AR arrow to navigate the optimal excaping path. The cell in 2D map inform users where the hazards are. User-friendly AR arrow inform users the direction to the exit.
* **Real-Time socket communication and Optimized Route Algorithm.** BEST uses TCP connection for real-time. Q-learning algorithm derives optimal path based on user's location and indoor situation.
    
## Documentation
To get more detail information about BEST, please refer to the [BEST documentation](https://github.com/BeaconAR/BEST/wiki). This describes key novelties on BEST, from what BEST is for, when BEST is useful.

To get more detail information about technology used in BEST, we have written a [series of documentation](https://github.com/BeaconAR/BEST/wiki) that explains how the BEST works, how did we troubleshoot.

## Installation for iOS
### Requirements
- iOS 13.0+ 
- Xcode 13.1+
- Swift 5.5+
- CocoaPods 1.11.3
- iOS Real Device (Recommended: iPhone 12)
### Dependencies
- **Firebase_iOS_sdk 9.2**
  -  Firebase Cloud Messaging for APNS(Apple Push Notification System)
  -  FirebaseMLModelDownloader for Deep Learning Model
  
- **TensorflowLiteSwift 2.9.1**
  -  TensorflowLiteSwift for Model interpreter
### Installation
- Dependencies installation
```
    pod install --repo-update
```

## Installation for Android

## Installation for Server

## Installation for IoT

## Related projects or Paper

## License
```
MIT License

Copyright (c) 2022 Bacon Beacon

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```


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

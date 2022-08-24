# Beacon-based Indoor Fire Evacuation System using Augmented Reality and Machine Learning

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

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
### Dependencies
-**Google Cloud Platform or Amazon AWS needed**
  - Python version 3.9
  - Firewall: Port 12000 and 5000 should be permitted for data transfer
## Installation for IoT
-**Raspberry Pi**
  - Raspbian OS
  - DHT11 Temperature and Humidity sensor
  - pybluez, bluez-hcidump should be installed
  - Programming Language: Python 3.9
-**Arduino**
  - ESP32 are recommended
  - DHT11 Temperature and Humidity sensor
  - Programming Language: C++
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

# AR-Evacuation with Beacons

![Generic badge](https://img.shields.io/badge/Xcode-13.3.1-blue.svg)  ![Generic badge](https://img.shields.io/badge/iOS-13.0-yellow.svg)  ![Generic badge](https://img.shields.io/badge/Swift-5.5-green.svg)  ![Generic badge](https://img.shields.io/badge/Firebase-ios-sdk.svg)  ![Generic badge](https://img.shields.io/badge/Socket.IO-15.0-purple.svg)   ![Generic badge](https://img.shields.io/badge/TensorflowliteSwift-2.9.1-orange.svg)

AR-Evacuation With Beacons is the Evacuation support Application with Beacons. AR-Evacuation With Beacons detects the beacons inside of [K-SW Square](https://m2m.tech.purdue.edu/) building which is in the Purdue University. The application estimates user's current location indoor and inform the optimized path to each user to flee safely from fire situation.
<img src="https://user-images.githubusercontent.com/69891604/184859167-452ede1a-c84c-4631-9c0c-c539267ab04f.PNG" width="200" height="400"/>
<img src="https://user-images.githubusercontent.com/69891604/184859158-24189076-b67f-47e6-b57b-6d0d379bbe42.PNG" width="200" height="400"/>
<img src="https://user-images.githubusercontent.com/69891604/184859150-8ef583b9-0fb2-4395-9435-5cd54b76807b.PNG" width="200" height="400"/>
----------------
## Requirements
- iOS 13.0+ 
- Xcode 13.1+
- Swift 5.5+
- CocoaPods 1.11.3
- iOS Real Device (Recommended: iphone 12)

## Dependencies
- **Firebase_iOS_sdk 9.2**
  -  Firebase Cloud Messaging for APNS(Apple Push Notification System)
  -  FirebaseMLModelDownloader for Deep Learning Model
  
- **TensorflowLiteSwift 2.9.1**
  -  TensorflowLiteSwift for Model interpreter

## Installation

- Dependencies installation
```
    pod install --repo-update
```

### 📎 ARNavigationViewController
ARNavigationViewController mainly displays the AR Navigation system. The application is made of MVC pattern. So this viewcontroller does lots of works. Mainly this viewcontroller change angle of 3D AR arrow and spawn the 3d object in next route based on given angle and distance from VectorService and IndoorLocationManager. With these information, the app can help evacuee flee safely.

This code block is main function of the viewcontroller. When the vector is set, the ARKit make the arrow change the angle in every 60 frames. The important things in AR world is that the app is using the gravityAndheading world configuration. With this, we are able to use absolute orientation, which means we can make our own 2d coordinate systems.
```swift
@objc private func movenotification(_ noti: Notification) {
        guard let userLocation = noti.object as? Position else {return}
        mapContentScrollView.scroll(to: map2DViewController.annotationView.currentPoint)
        
        // 1.when the path is there
        guard !path.path.isEmpty else {return}
        // 2. find the location
        guard let index = path.path.firstIndex(of: userLocation) else {return}
        // 3. if the location is not the destination
        if index < path.path.count - 1 {
            // find the vector bwt 2 points
            let start = VectorService.transformCellToCGPoint(cellname: path.path[index])
            let end = VectorService.transformCellToCGPoint(cellname:path.path[index+1])
            let vector = VectorService.vectorBetween2Points(from: start, to: end)
            
            // get angle
            directionDegree = vector.angle
            // get dist
            let dist = vector.dist
            
            // generate sphere to next cell
            // 1 cell = 36cm
            let newnode = generateSphereNode()
            sceneView.scene.rootNode.addChildNode(newnode)
            newnode.position = SCNVector3(x: arrow.position.x, y: arrow.position.y, z: arrow.position.z - (Float(dist) / 10 * 0.36) + 1.0)
            
        } else {
            bannerText = "Safely Exit"
            let alert = UIAlertController(title: "YOU ARE SAFE😀", message: nil, preferredStyle: .alert)
            self.present(alert, animated: true)
        }
        
    }
```

### 📎 Map2DViewController
Map2DViewController can user see the 2D map. Map2DViewController contains 3 views drawn by UIBeizerPath and 1 view that annotate user's location. The view of viewcontroller is ported in the scrollview inside ARNavigationViewController. Also, Map2DViewController can display the status of fire situation. Green cell is the optimized path. Red cell is the fire cell. Orange Cell is the fire nearby cell. Yellow cell is the conjestion cell. 

### 📎 IndoorLocationManager
IndoorLocationManager manage the beacon part and heading part from CoreLocation. The app use trained model to estimate the user's location with beacons' RSSI. Also, in order to adjust the user's location, we fusioned the result of the model and compass sensor. 
```swift
private func filterErrorWithHeading(previousLocation: Position, currentLocation: Position) -> Position {
        
        if previousLocation == .unknown || currentLocation == .unknown {return .unknown}
        
        let adjacentCells = previousLocation.adjacentCell.flatMap { (ele: [Position]) -> [Position] in
            return ele
        }
        
        // is CurrentLocation is Adjacent with previousLocation?
        let currentDirection = headingToDirection(heading: self.heading)
        if adjacentCells.contains(currentLocation) {

            var candidateCells = previousLocation.adjacentCell[currentDirection.rawValue]
            candidateCells.removeAll { $0 == .unknown }
            // if candidate cell and direction and heading are same?, that is answer
            if candidateCells.contains(currentLocation) {
                return currentLocation
            } else {
                // if adjacent cell, direction and heading not same, go previous cell
                return previousLocation
            }
        } else {
           // if not adjacent cell, but heading of user and angle between previous location and the result are same, we judge the user moves fast.
            let currentPosPoint = VectorService.transformCellToCGPoint(cellname: currentLocation)
            let prevPosPoint = VectorService.transformCellToCGPoint(cellname: previousLocation)
            let direction = headingToDirection(heading: Double(VectorService.vectorBetween2Points(from: prevPosPoint, to: currentPosPoint).angle))
            
            // then user can go to candidate cell with same direction
            if direction == currentDirection {
                var candidateCells = previousLocation.adjacentCell[direction.rawValue]
                candidateCells.removeAll { $0 == .unknown }
                return candidateCells.first ?? previousLocation
            } else {
                return previousLocation
            }
            
        }
    }
```

### 📎 KalmanFilter
Kalman Filter is used to smooth the RSSI from beacons. We reinit the filter when beacon's RSSI exceed the threshold. The Threshold will be selected by top 5% and bottom 5% in normal distribution.

### 📎 VectorService
In VectorService, we calculate the angle to direction in 2d coordinate. Also, we use atan2 function to find out angle between 2 points. 
```swift
static func vectorBetween2Points(from: CGPoint, to: CGPoint) -> (angle: Float, dist: Double) {
        var degree: Float = 0.0
        let tan = atan2(from.x - to.x, from.y - to.y) * 180 / .pi
        degree  = 180 - Float(tan)
        return (angle: degree, dist: sqrt(pow(from.x - to.x, 2) + pow(from.y - to.y, 2)))
    }
```


//
//  ARNavigationViewController.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/10/22.
//

import UIKit
import ARKit
import CoreLocation

enum AppState: Int16 {
    case DetectSurface  // Scan surface (Plane Detection On)
    case PointAtSurface // Point at surface to see focus point (Plane Detection Off)
    case TapToStart     // Focus point visible on surface, tap to start
    case Started
}

struct MapCell {
    var id: String
}


class ARNavigationViewController: UIViewController, ARSCNViewDelegate {
    
    var map = [[Int]](repeating: [1,1,1,1,1], count: 5)
    
    @IBOutlet weak var sceneView: ARSCNView!
    @IBOutlet weak var startButton: UIButton!
    @IBOutlet weak var resetButton: UIButton!
    
    var navStartPosition: SCNVector3?
    var navEndPosition: SCNVector3?
    
    var trackingStatus: String = ""
    var statusMessage: String = ""
    
//    var startNode: SCNNode!
//    var focusNode: SCNNode!
//    var focusPoint: CGPoint!
    var appState: AppState = .DetectSurface {
        didSet {
            print(appState)
        }
    }
    
    let configuration = ARWorldTrackingConfiguration()
    
    var vectorArr = [SCNVector3]()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.initScene()
        self.initCoachingOverlayView()
        self.initARSession()
//        self.initFocusNode()
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        sceneView.session.pause()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
    }
    
    func generateSphereNode() -> SCNNode {
        let sphere = SCNSphere(radius: 0.05)
        let sphereNode = SCNNode()
        sphereNode.position.y += Float(sphere.radius)
        sphereNode.geometry = sphere
        return sphereNode
    }
    
    @IBAction func saveButton(_ sender: UIButton) {
        CSVService.saveCSV(with: CSVService.arrToCSV(arr: vectorArr))
    }
    
    func renderer(_ renderer: SCNSceneRenderer, didAdd node: SCNNode, for anchor: ARAnchor) {
//        guard !(anchor is ARPlaneAnchor) else {
//            print("this is not planeAnchor")
//            return }
        print("여기찍힘")
//        guard let name = anchor.name else {return}
        let sphereNode = generateSphereNode()
//        guard let x = Float(name.split(separator: ",")[0]) else {
//            print("0 error")
//            return}
//        guard let y = Float(name.split(separator: ",")[1]) else {
//            print("1 error")
//            return}
//        guard let z = Float(name.split(separator: ",")[2]) else {
//            print("2 error")
//            return}
//        sphereNode.position.x = x
//        sphereNode.position.y = y
//        sphereNode.position.z = z
        DispatchQueue.main.async {
            node.addChildNode(sphereNode)
        }
    }
    
    @IBAction func startButtonPressed(_ sender: UIButton) {
        
//        guard appState == .TapToStart else { return }
//        self.startNode.isHidden = false
//        self.focusNode.isHidden = true
//        self.startNode.position = self.focusNode.position
//        navStartPosition = self.startNode.position
//
    
        let nodePositions = setMap()
        var firstNodePosition = SCNVector3(0, 0, 0)
        
        
        for i in nodePositions.indices {
            
            let sphere = generateSphereNode()
            sphere.position = nodePositions[i]
            
            sceneView.scene.rootNode.addChildNode(sphere)
//            let anchor = ARAnchor(transform: sphere.simdTransform * sceneView.scene.rootNode.simdWorldTransform)
//            sceneView.session.add(anchor: anchor)
            
//            if i == 0 {
//                firstNodePosition = nodePositions[i]
//                let translation = simd_float4x4 (
//                    float4(1, 0, 0, 0),
//                    float4(0, 1, 0, 0),
//                    float4(0, 0, 1, 0),
//                    float4(firstNodePosition.x, firstNodePosition.y, firstNodePosition.z, 1)
//                )
//                let anchor = ARAnchor(transform: translation)
//                sceneView.session.add(anchor: anchor)
//            } else {
//                let translation = simd_float4x4 (
//                    float4(1, 0, 0, 0),
//                    float4(0, 1, 0, 0),
//                    float4(0, 0, 1, 0),
//                    float4(nodePositions[i].x - firstNodePosition.x, nodePositions[i].y - firstNodePosition.y, nodePositions[i].z - firstNodePosition.z, 1)
//                )
//                let anchor = ARAnchor(transform: translation)
//                sceneView.session.add(anchor: anchor)
//            }
        
//            startNode.addChildNode(sphere)
            
        }
        
        sceneView.scene.rootNode.childNodes.forEach { node in
            let anchor = ARAnchor(transform: node.simdWorldTransform)
            sceneView.session.add(anchor: anchor)
        }
        sceneView.scene.rootNode.childNodes.forEach { node in
            node.removeFromParentNode()
        }
    }
    
    // 2 ~ 13까지는 1이랑 뺀다.
    // 2 ~ 13까지
    
    @IBAction func resetButtonPressed(_ sender: UIButton) {
        self.resetApp()
    }
    

}

// MARK: - App Status

extension ARNavigationViewController {
    
    func startApp() {
        DispatchQueue.main.async {
//            self.startNode.isHidden = true
//            self.focusNode.isHidden = true
            self.appState = .DetectSurface
        }
    }
    
    func resetApp() {
        DispatchQueue.main.async {
//            self.startNode.isHidden = true
            self.resetARSession()
            self.appState = .DetectSurface
        }
    }
}

// MARK: - AR Coaching Overlay

extension ARNavigationViewController: ARCoachingOverlayViewDelegate {
    
    func initCoachingOverlayView() {
        let coachingOverlay = ARCoachingOverlayView()
        coachingOverlay.session = self.sceneView.session
        coachingOverlay.delegate = self
        coachingOverlay.translatesAutoresizingMaskIntoConstraints = false
        coachingOverlay.activatesAutomatically = true
        coachingOverlay.goal = .horizontalPlane
        self.sceneView.addSubview(coachingOverlay)
        
        NSLayoutConstraint.activate([
            NSLayoutConstraint(item:  coachingOverlay, attribute: .top, relatedBy: .equal,
                               toItem: self.view, attribute: .top, multiplier: 1, constant: 0),
            NSLayoutConstraint(item:  coachingOverlay, attribute: .bottom, relatedBy: .equal,
                               toItem: self.view, attribute: .bottom, multiplier: 1, constant: 0),
            NSLayoutConstraint(item:  coachingOverlay, attribute: .leading, relatedBy: .equal,
                               toItem: self.view, attribute: .leading, multiplier: 1, constant: 0),
            NSLayoutConstraint(item:  coachingOverlay, attribute: .trailing, relatedBy: .equal,
                               toItem: self.view, attribute: .trailing, multiplier: 1, constant: 0)
        ])
    }
    
    func coachingOverlayViewWillActivate(_ coachingOverlayView: ARCoachingOverlayView) {
    }
    
    func coachingOverlayViewDidDeactivate(_ coachingOverlayView: ARCoachingOverlayView) {
        self.startApp()
    }
    
    func coachingOverlayViewDidRequestSessionReset(_ coachingOverlayView: ARCoachingOverlayView) {
        self.resetApp()
    }
}

// MARK: - AR Session Management (ARSCNViewDelegate)

extension ARNavigationViewController {
    
    func initARSession() {
        configuration.worldAlignment = .gravityAndHeading
        configuration.planeDetection = .horizontal
//        configuration.environmentTexturing = .automatic
        self.sceneView.session.run(configuration)
    }
    
    func resetARSession() {
        let config = sceneView.session.configuration as! ARWorldTrackingConfiguration
        config.planeDetection = .horizontal
        sceneView.session.run(config, options: [.resetTracking, .removeExistingAnchors])
    }
    
    func session(_ session: ARSession, cameraDidChangeTrackingState camera: ARCamera) {
        switch camera.trackingState {
        case .notAvailable:
            self.trackingStatus = "Tracking:  Not available!"
        case .normal:
            self.trackingStatus = "normal"
        case .limited(let reason):
            switch reason {
            case .excessiveMotion:
                self.trackingStatus = "Tracking: Limited due to excessive motion!"
            case .insufficientFeatures:
                self.trackingStatus = "Tracking: Limited due to insufficient features!"
            case .relocalizing:
                self.trackingStatus = "Tracking: Relocalizing..."
            case .initializing:
                self.trackingStatus = "Tracking: Initializing..."
            @unknown default:
                self.trackingStatus = "Tracking: Unknown..."
            }
        }
    }
    
    func session(_ session: ARSession, didFailWithError error: Error) {
        self.trackingStatus = "AR Session Failure: \(error)"
    }
    
    func sessionWasInterrupted(_ session: ARSession) {
        self.trackingStatus = "AR Session Was Interrupted!"
    }
    
    func sessionInterruptionEnded(_ session: ARSession) {
        self.trackingStatus = "AR Session Interruption Ended"
    }
}

// MARK: - Scene Management

extension ARNavigationViewController {
    
    func initScene() {
        self.sceneView.delegate = self
        //sceneView.showsStatistics = true
        sceneView.debugOptions = [
            ARSCNDebugOptions.showFeaturePoints,
            ARSCNDebugOptions.showWorldOrigin
            //SCNDebugOptions.showBoundingBoxes,
            //SCNDebugOptions.showWireframe
        ]
        
//        startNode = generateSphereNode()
//        startNode.isHidden = true
//        sceneView.scene.rootNode.addChildNode(startNode)
    }
    
//    func renderer(_ renderer: SCNSceneRenderer, updateAtTime time: TimeInterval) {
//        DispatchQueue.main.async {
////            self.updateFocusNode()
//            self.updateStatus()
//        }
//    }
    
    func updateStatus() {
        switch appState {
        case .DetectSurface:
            statusMessage = "Scan available flat surfaces..."
        case .PointAtSurface:
            statusMessage = "Point at designated surface first!"
        case .TapToStart:
            statusMessage = "Tap to start."
        case .Started:
            statusMessage = "Tap objects for more info."
        }
        
    }
    
}

extension ARNavigationViewController {
    
//    @objc func orientationChanged() {
//        focusPoint = CGPoint(x: view.center.x, y: view.center.y  + view.center.y * 0.1)
//    }
    
//    func initFocusNode() {
//        focusNode = generateSphereNode()
//        focusNode.isHidden = true
//        sceneView.scene.rootNode.addChildNode(focusNode)
//
//        focusPoint = CGPoint(x: view.center.x, y: view.center.y + view.center.y * 0.1)
//        NotificationCenter.default.addObserver(self, selector: #selector(ARNavigationViewController.orientationChanged), name: UIDevice.orientationDidChangeNotification, object: nil)
//    }
    
//    func updateFocusNode() {
//
//        guard appState != .Started else {
//            focusNode.isHidden = true
//            return
//        }
//
//        if let query = self.sceneView.raycastQuery(from: self.focusPoint, allowing: .estimatedPlane, alignment: .horizontal) {
//            let results = self.sceneView.session.raycast(query)
//
//            if results.count == 1 {
//                if let match = results.first {
//                    let t = match.worldTransform
//                    self.focusNode.position = SCNVector3(x: t.columns.3.x, y: t.columns.3.y, z: t.columns.3.z)
//                    self.appState = .TapToStart
//                    focusNode.isHidden = false
//                }
//            } else {
//                self.appState = .PointAtSurface
//                focusNode.isHidden = true
//            }
//        }
//
//    }
}


// MARK: - Coordinate System
extension ARNavigationViewController {
    
    private func setMap() -> [SCNVector3] {
        let path = Bundle.main.path(forResource: "final_U01 2", ofType: "csv")!
        let path2 = Bundle.main.path(forResource: "source", ofType: "csv")
        var nodes = CSVService.parseCSVAt(url: URL(fileURLWithPath: path))
        var sourceNodes = CSVService.parseCSVAt(url: URL(fileURLWithPath: path))
        var positions: [SCNVector3] = []
        for i in nodes.indices {
            if nodes[i][2].contains("\r") {
                nodes[i][2].removeLast()
                nodes[i][2].removeLast()
            }
            let x = Float(nodes[i][0])!
            let y = Float(nodes[i][1])!
            let z = Float(nodes[i][2])!
            let position = SCNVector3(x: x, y: y, z: z)
            positions.append(position)
        }
        
        return positions
    }
    
    private func setCoordinateNode(start: (x: Int, y: Int), xlimit: Int, ylimit: Int) {

        var visited: [[Int]] = [[Int]](repeating: [Int](repeating: 0, count: xlimit), count: ylimit)
        var queue = [start]
        var index = 0
        var dx = [0,0,1,-1] // 위(북쪽), 아래(남쪽), 오른쪽(동쪽), 왼쪽(서쪽)
        var dy = [-1,1,0,0]
        
        while queue.count > index {
            let node = queue[index]
            for i in 0..<dx.count {
                let nextX = node.x + dx[i]
                let nextY = node.y + dy[i]
                
                if nextX < 0 || nextX >= xlimit || nextY < 0 || nextY >= ylimit {
                    continue
                }
                else {
                    if map[nextX][nextY] == 1 && (visited[nextX][nextY] == 0) {
                        queue.append((nextX, nextY))
                        visited[nextX][nextY] = 1
                        
                        let coordinateNode = generateSphereNode()
                        coordinateNode.name = "\(nextX), \(nextY)"
                        
//                        coordinateNode.position = SCNVector3(startNode.position.x + Float(nextY) - Float(start.y), startNode.position.y, startNode.position.z + Float(nextX) - Float(start.x))
                        print(coordinateNode.position)
                        coordinateNode.geometry?.firstMaterial?.diffuse.contents = UIColor.red
                        self.sceneView.scene.rootNode.addChildNode(coordinateNode)
                                                
                        // 여기서 AR에 박기
//                        answer[nextX][nextY] += answer[node.x][node.y] + 1
                    }
                }
            }
            
            index += 1
        }
    }
    
}

//func +(left: SCNVector3, right: SCNVector3) -> SCNVector3 {
//    return SCNVector3Make(left.x + right.x, left.y + right.y, left.z + right.z)
//}

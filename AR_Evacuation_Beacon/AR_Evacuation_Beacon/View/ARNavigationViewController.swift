//
//  ARNavigationViewController.swift
//  AR_Evacuation_Beacon
//
//  Created by Jung peter on 7/10/22.
//

import UIKit
import ARKit
import CoreLocation

final class ARNavigationViewController: UIViewController, ARSCNViewDelegate, CLLocationManagerDelegate {
    
    var map = [[Int]](repeating: [1,1,1,1,1], count: 5)
    
    @IBOutlet weak var sceneView: ARSCNView!
    
    private var map2Dview: UIView = UIView()
    private var map2DViewController = Map2DViewController()
    private let mapContentScrollView: UIScrollView = {
        let scrollView = UIScrollView()
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        scrollView.backgroundColor = .clear
        scrollView.bounces = false
        scrollView.showsVerticalScrollIndicator = true
        scrollView.showsHorizontalScrollIndicator = true
        return scrollView
    }()
    private var arrow = SCNNode()
    private var heading: Double = 360
    private var directionDegree: Float = 0
    private var path: [String] = []
    
    let locationManager: CLLocationManager = {
        $0.requestWhenInUseAuthorization()
        $0.startUpdatingHeading()
        return $0
    }(CLLocationManager())
    
    let configuration = ARWorldTrackingConfiguration()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.initScene()
        self.initARSession()
        self.set2DNavigationView()
        locationManager.delegate = self
        arrow = generateArrowNode()
        self.sceneView.scene.rootNode.addChildNode(arrow)
        
        // Add Observer
        NotificationCenter.default.addObserver(self, selector: #selector(movenotification(_:)), name: .movePosition, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(getPath(_:)), name: .path, object: nil)
    }
    
    @objc private func getPath(_ noti: Notification) {
        guard let path = noti.object as? [String] else {return}
        self.path = path
    }

}


// MARK: - AR Session Management (ARSCNViewDelegate)

extension ARNavigationViewController {
    
    func initARSession() {
        configuration.worldAlignment = .gravityAndHeading
        self.sceneView.session.run(configuration)
    }
    
    func resetARSession() {
        let config = sceneView.session.configuration as! ARWorldTrackingConfiguration
        sceneView.session.run(config, options: [.resetTracking, .removeExistingAnchors])
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        self.heading = newHeading.trueHeading
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
        ]
        
    }
    
    func renderer(_ renderer: SCNSceneRenderer, updateAtTime time: TimeInterval) {
        
        guard let pointOfView = sceneView.pointOfView else {return}
        let transform = pointOfView.transform
        let orientation = SCNVector3(-transform.m31, -transform.m32, -transform.m33)
        let location = SCNVector3(transform.m41, transform.m42, transform.m43)
        let currentPositionOfCamera = location + orientation
        
        DispatchQueue.main.async { [self] in
            self.arrow.position = SCNVector3(x: currentPositionOfCamera.x, y: currentPositionOfCamera.y + 0.1, z: currentPositionOfCamera.z)
            self.arrow.eulerAngles = SCNVector3(x: 0, y:changeDirection(degree: directionDegree), z: 0)
        }
        
    }
    
}

// MARK: - Private Function

extension ARNavigationViewController {
    
    private func changeDirection(degree: Float) -> Float {
        return Float(Float(270 - degree).degreesToRadians)
    }
    
    private func directionChange(degree: Float) {
        SCNTransaction.begin()
        SCNTransaction.animationDuration = 1
        let quaternion = simd_quatf(angle: GLKMathDegreesToRadians(degree), axis: simd_float3(0,0,1))
        arrow.simdOrientation = quaternion * arrow.simdOrientation
        SCNTransaction.commit()
    }
    
}

// MARK: - Create SCNNode
extension ARNavigationViewController {
    
    private func generateArrowNode() -> SCNNode {
        
        let vertexCount = 48;
        let vertexs: [Float] = [ -1.4923, 1.1824, 2.5000, -6.4923, 0.000, 0.000, -1.4923, -1.1824, 2.5000, 4.6077, -0.5812, 1.6800, 4.6077, -0.5812, -1.6800, 4.6077, 0.5812, -1.6800, 4.6077, 0.5812, 1.6800, -1.4923, -1.1824, -2.5000, -1.4923, 1.1824, -2.5000, -1.4923, 0.4974, -0.9969, -1.4923, 0.4974, 0.9969, -1.4923, -0.4974, 0.9969, -1.4923, -0.4974, -0.9969 ];
        
        let facecount = 13;
        let faces: [CInt] = [  3, 4, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 0, 1, 2, 3, 4, 5, 6, 7, 1, 8, 8, 1, 0, 2, 1, 7, 9, 8, 0, 10, 10, 0, 2, 11, 11, 2, 7, 12, 12, 7, 8, 9, 9, 5, 4, 12, 10, 6, 5, 9, 11, 3, 6, 10, 12, 4, 3, 11 ];
        
        let vertexData  = NSData(
            bytes: vertexs,
            length: MemoryLayout<Float>.size * vertexCount
        )
        
        let vertexSource = SCNGeometrySource(data: vertexData as Data,
                                             semantic: .vertex,
                                             vectorCount: vertexCount,
                                             usesFloatComponents: true,
                                             componentsPerVector: 3,
                                             bytesPerComponent: MemoryLayout<Float>.size,
                                             dataOffset: 0,
                                             dataStride: MemoryLayout<Float>.size * 3)
        
        let polyIndexCount = 61;
        let indexPolyData  = NSData( bytes: faces, length: MemoryLayout<CInt>.size * polyIndexCount )
        
        let element = SCNGeometryElement(data: indexPolyData as Data,
                                          primitiveType: .polygon,
                                          primitiveCount: facecount,
                                          bytesPerIndex: MemoryLayout<CInt>.size)
        
        let geometry = SCNGeometry(sources: [vertexSource], elements: [element])
        
        let material = geometry.firstMaterial!
        
        material.diffuse.contents = UIColor(red: 0.14, green: 0.82, blue: 0.95, alpha: 1.0)
        material.lightingModel = .lambert
        material.transparency = 1.00
        material.transparencyMode = .dualLayer
        material.fresnelExponent = 1.00
        material.reflective.contents = UIColor(white:0.00, alpha:1.0)
        material.specular.contents = UIColor(white:0.00, alpha:1.0)
        material.shininess = 1.00
        
        //Assign the SCNGeometry to a SCNNode, for example:
        let arrow = SCNNode()
        arrow.geometry = geometry
        arrow.scale = SCNVector3(0.05, 0.05, 0.05)
        arrow.eulerAngles = SCNVector3(x: 0, y: Float(Float(heading - 90).degreesToRadians), z: 0)
        arrow.name = "arrow"
        arrow.geometry?.firstMaterial?.diffuse.contents = UIColor.systemBlue
        
        return arrow
    }
    
    
    private func generateSphereNode() -> SCNNode {
        let sphere = SCNSphere(radius: 0.05)
        let sphereNode = SCNNode()
        sphereNode.position.y += Float(sphere.radius)
        sphereNode.geometry = sphere
        return sphereNode
    }
    
}

// MARK: - UI configuration

extension ARNavigationViewController {
    
    private func set2DNavigationView() {
        view.addSubview(mapContentScrollView)
        mapContentScrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        mapContentScrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        mapContentScrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        mapContentScrollView.heightAnchor.constraint(equalToConstant: view.frame.height / 2.5).isActive = true
        setMap2dViewController()
    }
    
    private func setMap2dViewController() {
        guard let map2dView = map2DViewController.view else {return}
        map2dView.translatesAutoresizingMaskIntoConstraints = false
        self.map2Dview = map2dView
        mapContentScrollView.addSubview(self.map2Dview)
        
        // constraint
        self.map2Dview.widthAnchor.constraint(equalTo: view.widthAnchor).isActive = true
        self.map2Dview.heightAnchor.constraint(equalTo: view.heightAnchor).isActive = true
        
        self.map2Dview.leadingAnchor.constraint(equalTo: mapContentScrollView.leadingAnchor).isActive = true
        self.map2Dview.trailingAnchor.constraint(equalTo: mapContentScrollView.trailingAnchor).isActive = true
        self.map2Dview.topAnchor.constraint(equalTo: mapContentScrollView.topAnchor).isActive = true
        self.map2Dview.bottomAnchor.constraint(equalTo: mapContentScrollView.bottomAnchor).isActive = true
    }
    
    @objc private func movenotification(_ noti: Notification) {
        guard let userLocation = noti.object as? String else {return}
        mapContentScrollView.scroll(to: map2DViewController.annotationView.currentPoint)
        
        // 1.path가 들어왔다.
        guard !path.isEmpty else {return}
        // 2.현재위치를 파악한다.
        guard let index = path.firstIndex(of: userLocation) else {return}
        if index < path.count - 1 {
            // 다음꺼의 거리를 찾기
            let start = transformCellToCGPoint(cellname: path[index])
            let end = transformCellToCGPoint(cellname:path[index+1])
            let vector = vectorBetween2Points(from: start, to: end)
            
            // get angle
            directionDegree = vector.angle
            // get dist
            let dist = vector.dist
            
            // generate sphere to next cell
            // 1 cell = 36cm
            let newnode = generateSphereNode()
            sceneView.scene.rootNode.addChildNode(newnode)
            newnode.position = SCNVector3(x: arrow.position.x, y: arrow.position.y, z: arrow.position.z - (Float(dist) / 10 * 0.36) + 1.0)
            
        }
        
    }
    
    private func transformCellToCGPoint(cellname: String) -> CGPoint {
        
        var start: (x: CGFloat, y: CGFloat) = (0, 0)
        var end: (x: CGFloat, y: CGFloat) = (0, 0)
        
        if let firstFloorCellpoints = mapDic[cellname] {
            start = firstFloorCellpoints[0]
            end = firstFloorCellpoints[2]
        } else if let secondFloorCellpoints = micDic2[cellname] {
            start = secondFloorCellpoints[0]
            end = secondFloorCellpoints[2]
        } else if let baseFloorCellPoints = micDic0[cellname] {
            start = baseFloorCellPoints[0]
            end = baseFloorCellPoints[2]
        } else {
            return CGPoint(x: 0, y: 0)
        }
    
        let width = abs(start.x - end.x) / 2
        let height = abs(start.y - end.y) / 2
        
        return CGPoint(x: (start.x + width) * 10, y: (start.y + height) * 10)
    }
    
    private func vectorBetween2Points(from: CGPoint, to: CGPoint) -> (angle: Float, dist: Double) {
        var degree: Float = 0.0
        let tan = atan2(from.x - to.x, from.y - to.y) * 180 / .pi
        if tan < 0 {
            degree = Float(-tan) + 180.0
        } else {
            degree = 180.0 - Float(tan)
        }
        return (angle: degree, dist: sqrt(pow(from.x - to.x, 2) + pow(from.y - to.y, 2)))
        
    }

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

func +(left: SCNVector3, right: SCNVector3) -> SCNVector3 {
    return SCNVector3Make(left.x + right.x, left.y + right.y, left.z + right.z)
}

extension Float {
    
    var degreesToRadians: Float { return Float(self) * .pi/180}
}

extension Int {
    
    var degreesToRadians: Double { return Double(self) * .pi/180}
}

extension UIScrollView {
    func scroll(to point: CGPoint) {
        let y = point.y - (self.frame.height / 2) < 0 ? 0 : point.y - (self.frame.height / 2)
        self.setContentOffset(CGPoint(x: 0, y: y), animated: true)
    }
}

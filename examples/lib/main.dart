import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:plugin_filters/camera_view.dart';
import 'package:plugin_filters/plugin_filters.dart';

void main() {
  runApp(const MaterialApp(
    title: 'CameraApp',
    home: MyApp(),
  ));
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  late PluginFilters cameraFlutterPluginDemo;
  late CameraView cameraView;
  static const platform = MethodChannel("plugin_filters");

  @override
  void initState() {
    super.initState();
    requestPermission();
    cameraView = CameraView(
      onCreated: onCameraViewCreated,
    );
    platform.setMethodCallHandler((call) => _methodHandler(call));
  }

  Future<void> requestPermission() async {
    await Permission.camera.request();
    await Permission.storage.request();
    await Permission.manageExternalStorage.request();
  }

  Future<dynamic> _methodHandler(MethodCall call) async {
    switch (call.method) {
      case "switchCamera":
        debugPrint(call.arguments);
        return Future.value("switchCamera");
    }
  }

  void onCameraViewCreated(cameraFlutterPluginDemo) {
    this.cameraFlutterPluginDemo = cameraFlutterPluginDemo;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Underpin"),
      ),
      body: Stack(
        alignment: Alignment.bottomCenter,
        children: [
          cameraView,
          Positioned(
            bottom: 30,
            left: 20,
            right: 20,
            child: GestureDetector(
              onTap: () {},
              child: Container(
                width: 80,
                height: 80,
                alignment: Alignment.center,
                decoration: const BoxDecoration(
                  shape: BoxShape.circle,
                  color: Colors.blueGrey,
                ),
                child: const Icon(
                  Icons.camera,
                  size: 60,
                  color: Colors.black87,
                ),
              ),
            ),
          ),
          Positioned(
            bottom: 50,
            left: 100,
            child: GestureDetector(
              onTap: () {
                cameraFlutterPluginDemo.switchCamera();
              },
              child: Container(
                width: 40,
                height: 40,
                alignment: Alignment.center,
                decoration: const BoxDecoration(
                  shape: BoxShape.circle,
                  color: Colors.grey,
                ),
                child: const Icon(
                  Icons.cameraswitch,
                  size: 26,
                  color: Colors.black87,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

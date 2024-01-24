import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'plugin_filters.dart';

class CameraView extends StatefulWidget {
  final PluginCamCallback? onCreated;
  const CameraView({Key? key, this.onCreated}) : super(key: key);

  @override
  State<CameraView> createState() => _CameraViewState();
}

class _CameraViewState extends State<CameraView> {
  @override
  Widget build(BuildContext context) {
    return AnimatedOpacity(
      opacity: 1.0,
      duration: const Duration(milliseconds: 2000),
      child: Container(
        color: Colors.white,
        child: _loadNativeView(),
      ),
    );
  }

  Widget _loadNativeView() {
    if (Platform.isAndroid) {
      return AndroidView(
        viewType: 'plugin_filters',
        layoutDirection: TextDirection.ltr,
        onPlatformViewCreated: onPlatformViewCreated,
        creationParams: const <String, dynamic>{
        },

        creationParamsCodec: const StandardMessageCodec(),
      );
    } else if (Platform.isIOS) {
      return UiKitView(
        viewType: 'plugin_filters',

        onPlatformViewCreated: onPlatformViewCreated,

        creationParamsCodec: const StandardMessageCodec(),
      );
    } else {
      return const Text('暂不支持其他平台');
    }
  }

  Future<void> onPlatformViewCreated(id) async {
    if (widget.onCreated == null) {
      return;
    }
    widget.onCreated!(PluginFilters());
  }
}

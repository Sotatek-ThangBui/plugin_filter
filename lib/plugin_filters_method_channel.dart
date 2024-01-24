import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'plugin_filters_platform_interface.dart';

/// An implementation of [PluginFiltersPlatform] that uses method channels.
class MethodChannelPluginFilters extends PluginFiltersPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('plugin_filters');

  @override
  Future<void> switchCamera() {
    return methodChannel.invokeMethod('switchCamera');
  }

  @override
  Future<void> selectedFilter(int filter) {
    return methodChannel.invokeMethod('setFilter', <String, int>{
      'filter_selected': filter,
    });
  }
}

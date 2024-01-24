import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'plugin_filters_method_channel.dart';

abstract class PluginFiltersPlatform extends PlatformInterface {
  /// Constructs a PluginFiltersPlatform.
  PluginFiltersPlatform() : super(token: _token);

  static final Object _token = Object();

  static PluginFiltersPlatform _instance = MethodChannelPluginFilters();

  /// The default instance of [PluginFiltersPlatform] to use.
  ///
  /// Defaults to [MethodChannelPluginFilters].
  static PluginFiltersPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [PluginFiltersPlatform] when
  /// they register themselves.
  static set instance(PluginFiltersPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<void> switchCamera() {
    throw UnimplementedError('switchCamera() has not been implemented.');
  }

  Future<void> selectedFilter(int filter) {
    throw UnimplementedError('selectedFilter() has not been implemented.');
  }
}

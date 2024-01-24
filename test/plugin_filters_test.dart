import 'package:flutter_test/flutter_test.dart';
import 'package:plugin_filters/plugin_filters.dart';
import 'package:plugin_filters/plugin_filters_platform_interface.dart';
import 'package:plugin_filters/plugin_filters_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockPluginFiltersPlatform
    with MockPlatformInterfaceMixin
    implements PluginFiltersPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final PluginFiltersPlatform initialPlatform = PluginFiltersPlatform.instance;

  test('$MethodChannelPluginFilters is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelPluginFilters>());
  });

  test('getPlatformVersion', () async {
    PluginFilters pluginFiltersPlugin = PluginFilters();
    MockPluginFiltersPlatform fakePlatform = MockPluginFiltersPlatform();
    PluginFiltersPlatform.instance = fakePlatform;

    expect(await pluginFiltersPlugin.getPlatformVersion(), '42');
  });
}

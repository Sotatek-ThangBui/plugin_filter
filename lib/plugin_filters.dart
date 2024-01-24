
import 'plugin_filters_platform_interface.dart';
typedef PluginCamCallback = void Function(PluginFilters controller);
class PluginFilters {
  Future<void> switchCamera() {
    return PluginFiltersPlatform.instance.switchCamera();
  }

  Future<void> selectedFilter(int filter) {
    return PluginFiltersPlatform.instance.selectedFilter(filter);
  }
}

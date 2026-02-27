require 'json'

package = JSON.parse(File.read(File.join(__dir__, '..', 'package.json')))

Pod::Spec.new do |s|
  s.name           = 'ExpoKLineChart'
  s.version        = package['version']
  s.summary        = 'Native K-Line chart module for Expo'
  s.description    = 'A high-performance native K-Line (candlestick) chart view for React Native via Expo Modules API'
  s.homepage       = 'https://github.com/example/expo-kline-chart'
  s.license        = package['license']
  s.author         = 'expo-kline-chart'
  s.platforms      = { :ios => '15.1' }
  s.source         = { git: '' }
  s.static_framework = true

  s.dependency 'ExpoModulesCore'

  s.source_files = '**/*.swift'
end

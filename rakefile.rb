#!/usr/bin/ruby -e
require 'bundler/setup'
require 'fileutils'
require 'rake/clean'
require 'json'
require 'net/http'
require 'travis-build-tools'

#Constants
PWD = File.dirname(__FILE__)
TARGET_DIR = File.join(PWD, 'target')
AMPS_DIR = File.join(PWD, 'amps-standalone')

#Tasks
task :default => [:package]

desc 'Download atlassian sdk'
task :setup

desc 'Use Atlassian to build'
desc :build

desc 'Create the package'
task :package => [:setup, :build]

desc 'builds the plugin'
task :compile

desc 'Builds and runs the plugin'
task :run

desc 'Creates eclipse project'
task :project

task :clobber => [:clean]
CLOBBER.include(TARGET_DIR, AMPS_DIR)

ATLASSIAN_TOOLS = File.join(PWD, 'atlassian-tools')
directory ATLASSIAN_TOOLS
task :setup => [ATLASSIAN_TOOLS] do
  remote_location = 'https://marketplace.atlassian.com/download/plugins/atlassian-plugin-sdk-tgz'
  local_path = File.join(ATLASSIAN_TOOLS, 'atlassian.tgz')
  uri = URI(remote_location)
  Net::HTTP.start(uri.host, uri.port) do |http|
    request = Net::HTTP::Get.new(uri.request_uri)
    http.request(request) do |response|
      fileTotalSize = response["content-length"]
      open(local_path, 'wb') do |streamFile|
        response.read_body do |chunk|
          streamFile.write chunk
          print ((File.size(streamFile).to_f / fileTotalSize.to_f) * 100).round(2).to_s + " % \r"
        end
      end
    end
  end
end

task :build do
  
end

task :compile do
  cmd = 'mvn -DdownloadSources=trueclean compile assembly:single'
  system(cmd)
end

task :run do
  cmd = 'atlas-run'
  system(cmd)
end

task :project do
  FileUtils.rm_rf(File.join(PWD, '.classpath'))
  FileUtils.rm_rf(File.join(PWD, '.project'))
  cmd = 'mvn -DdownloadSources=true eclipse:eclipse'
  system(cmd)
end

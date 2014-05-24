#!/usr/bin/ruby -e
require 'rake'
require 'rake/clean'
require 'fileutils'

#Constants
PWD = File.dirname(__FILE__)
TARGET_DIR = File.join(PWD, 'target')
AMPS_DIR = File.join(PWD, 'amps-standalone')

#Tasks
task :default => [:run]

desc 'builds the plugin'
task :compile

desc 'Builds and runs the plugin'
task :run

desc 'Creates eclipse project'
task :project

task :clobber => [:clean]
CLOBBER.include(TARGET_DIR, AMPS_DIR)

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

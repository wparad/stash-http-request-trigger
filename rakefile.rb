#!/usr/bin/ruby -e
require 'bundler/setup'
require 'fileutils'
require 'rake/clean'
require 'json'
require 'net/http'
require 'rest-client'
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

task :after_build => [:publish_git_tag, :display_repository]

task :clobber => [:clean]
CLOBBER.include(TARGET_DIR, AMPS_DIR)

ATLASSIAN_TOOLS_DIR = File.join(PWD, 'atlassian-tools')
file ATLASSIAN_TOOLS_DIR do
  FileUtils.mkdir_p(ATLASSIAN_TOOLS_DIR)
  remote_location = 'https://marketplace.atlassian.com/download/plugins/atlassian-plugin-sdk-tgz'
  atlassian_tools = File.join(ATLASSIAN_TOOLS_DIR, 'atlassian.tgz')
  download_file(atlassian_tools, remote_location)
  Dir.chdir(ATLASSIAN_TOOLS_DIR) do
    puts %x[tar -xzvf #{File.basename(atlassian_tools)}]
  end
end
task :setup => [ATLASSIAN_TOOLS_DIR]

def download_file(local_path, remote_location)
  url = URI::encode(remote_location)
  begin
    response = RestClient.get(url)

    FileUtils.mkdir_p(File.dirname(local_path))
    File.open(local_path, 'wb'){|file| file.write(response.body)}
  rescue Exception => exception
    puts "Failed to download #{url}:"
    raise
  end
end

task :build do
  script = Dir[File.join(ATLASSIAN_TOOLS_DIR, "**", 'atlas-package')].first
  puts %x[#{script}]
end

publish_git_tag :publish_git_tag do |t, args|
  t.git_repository = %x[git config --get remote.origin.url].split('://')[1]
  t.tag_name = TravisBuildTools::Build::VERSION
  t.service_user = ENV['GIT_TAG_PUSHER']
end

task :display_repository do
  puts Dir.glob(File.join(PWD, '**', '*'), File::FNM_DOTMATCH).select{|f| !f.match(/\/(\.git|vendor|bundle)\//)}
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

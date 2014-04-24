#!/usr/bin/ruby
=begin
# Author: Warren Parad
# Date: 20140424
=end

require 'json'
require 'uri'
require 'net/http'
require "cgi"
require 'fileutils'
puts "Content-type: text/html"
puts "Accept: application/json\r\n\r\n"

cgi = CGI.new
query_string = CGI.parse(cgi.query_string)
job_name = query_string['JOB_NAME'][0]
hash = cgi.params
json = JSON::parse(hash.keys[0])

success = false
begin
  ref_changes = json['refChanges']
  success = ref_changes && !ref_changes.empty?
rescue => message
  puts "Failed to part stash POST: #{message}"
end

if !success || job_name.nil?
  puts 'Missing Information'
  puts 'HTTP/1.0 404 missing informaion'
  exit 0
end

puts "Jenkins Job Name: #{job_name}"
branch = ref_changes[0]['refId']
sha = ref_changes[0]['toHash']

#write out job name hash and branch and validate they don't exist before triggering
file = File.join('/tmp', 'jenkins_trigger', job_name, "#{sha}-#{branch}")
FileUtils.mkdir_p(File.dirname(file))
exit if File.exists?(file)
File.write(file, '0')

#Trigger Jenkins here

#Skip is triggering doesn't work for 5 minutes
stop_at = Time.now + 60*5

while Time.now < stop_at
  jenkins_root = 'https://jenkins'
  jenkins_job_root = File.join(jenkins_root, 'job')
  uri = URI.parse(jenkins_root)
  http = Net::HTTP.new(uri.host, uri.port)
  http.use_ssl = true
  http.verify_mode = OpenSSL::SSL::VERIFY_NONE
  http.read_timeout = nil
  job_http = File.join(jenkins_job_root, job_name, "buildWithParameters?BUILD_CAUSE=Stash&STASH_COMMIT=#{sha}&STASH_REF=#{branch}&QUERY_STRING=#{query_string}")
  puts "Triggering Puppet Job: #{job_http}"

  user = "CHANGE THIS"
  password = "CHANGE THIS"
  request = Net::HTTP::Post.new(job_http)
  request.basic_auth(user, password)
  response = http.request(request)
  puts "Jenkins job update response: #{response.code} #{response.body}"
  
  break if response.code == '201'
  sleep 15

end

puts "HTTP/1.0 #{response.code} #{response.body}"

#!/usr/bin/env ruby

require 'rubygems'
require 'rest-client'
require 'json'

# Identification sur le web_service en tant que société
begin
  response = RestClient.post("https://ws.agiloffice.fr/demo/session_firm_login?password=xxxxxx", {:accept => 'application/json'})
  if response.code == 200
    puts "Ok : identification effectuée"
    cookie_str = response.headers[:set_cookie].first.split(';').first

    # Décrochage du poste 8003
    begin
      response = RestClient.post("https://ws.agiloffice.fr/demo/phone_answer?phone=8003", {}, {:accept => 'application/json', :cookie => cookie_str})
      if response.code == 200
        puts "Ok : décrochage du poste effectué : #{response.body}"
      else
        puts "Erreur #{response.code} : poste non décroché : #{response.body}"
      end
    rescue => e
      if e.is_a?(RestClient::Exception)
        puts "Erreur #{e.response.code} : poste non décroché : #{e.response.body}"
      else
        puts "Exception : #{e.message}\n" + e.backtrace.join("\n")
      end
    end

    # Raccrochage du poste 8003
    begin
      response = RestClient.post("https://ws.agiloffice.fr/demo/phone_hangup?phone=8003", {}, {:accept => 'application/json', :cookie => cookie_str})
      if response.code == 200
        puts "Ok : raccrochage du poste effectué : #{response.body}"
      else
        puts "Erreur #{response.code} : poste non raccroché : #{response.body}"
      end
    rescue => e
      if e.is_a?(RestClient::Exception)
        puts "Erreur #{e.response.code} : poste non raccroché : #{e.response.body}"
      else
        puts "Exception : #{e.message}\n" + e.backtrace.join("\n")
      end
    end
  else
    puts "Erreur #{response.code} : identification échouée : #{response.body}"
  end

  # Récupération de l'url du serveur de push
  begin
    response = RestClient.get("https://ws.agiloffice.fr/demo/push_server_url", {:accept => 'application/json', :cookie => cookie_str})
    if response.code == 200
      puts "Ok : url du push_server récupérée : #{response.body}"
    else
      puts "Erreur #{response.code} : url du push_server non récupérée : #{response.body}"
    end
  rescue => e
    if e.is_a?(RestClient::Exception)
      puts "Erreur #{e.response.code} : url du push_server non récupérée : #{e.response.body}"
    else
      puts "Exception : #{e.message}\n" + e.backtrace.join("\n")
    end
  end

  # Envoi d'un fax
  begin
    response = RestClient.post("https://ws.agiloffice.fr/demo/send_fax?from=0123456789&to=9876543210",
      {:file => File.new("#{File.dirname(__FILE__)}/document.pdf", 'rb')},
      {:accept => 'application/json', :cookie => cookie_str})
    if response.code == 200
      puts "Ok : fax envoyé : #{response.body}"
      fax_id = JSON.parse(response.body)['fax_id']
      # Récupération de l'état du fax envoyé
      begin
        response = RestClient.get("https://ws.agiloffice.fr/demo/get_fax_status?fax_id=#{fax_id}", {:accept => 'application/json', :cookie => cookie_str})
        if response.code == 200
          puts "Ok : statut du fax récupéré : #{response.body}"
        else
          puts "Erreur #{response.code} : statut du fax non récupérée : #{response.body}"
        end
      rescue => e
        if e.is_a?(RestClient::Exception)
          puts "Erreur #{e.response.code} : statut du fax non récupérée : #{e.response.body}"
        else
          puts "Exception : #{e.message}\n" + e.backtrace.join("\n")
        end
      end
    else
      puts "Erreur #{response.code} : fax non envoyé : #{response.body}"
    end
  rescue => e
    if e.is_a?(RestClient::Exception)
      puts "Erreur #{e.response.code} : fax non envoyé : #{e.response.body}"
    else
      puts "Exception : #{e.message}\n" + e.backtrace.join("\n")
    end
  end
rescue => e
  if e.is_a?(RestClient::Exception)
    puts "Erreur #{e.response.code} : identification échouée : #{e.response.body}"
  else
    puts "Exception : #{e.message}\n" + e.backtrace.join("\n")
  end
end


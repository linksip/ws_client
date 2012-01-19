#!/usr/bin/env python
# -*- coding: utf-8 -*-

from httplib2 import Http
from urllib import urlencode
import mimetools, mimetypes
from cStringIO import StringIO
import os, stat
import json

def multipart_encode(fd):
  boundary = mimetools.choose_boundary()
  buf = StringIO()
  file_size = os.fstat(fd.fileno())[stat.ST_SIZE]
  filename = fd.name.split('/')[-1]
  buf.write('--%s\r\n' % boundary)
  buf.write('Content-Disposition: form-data; name="%s"; filename="%s"\r\n' % ('file', filename))
  buf.write('Content-Type: application/pdf\r\n')
  fd.seek(0)
  buf.write('\r\n' + fd.read() + '\r\n')
  buf.write('--' + boundary + '--\r\n\r\n')
  buf = buf.getvalue()
  return boundary, buf

h = Http()

# Identification sur le web_service en tant que société
resp, content = h.request("http://ws.agiloffice.fr/demo/session_firm_login?password=xxxxxx", "POST", headers = {'Accept':'application/json'})
if resp.status == 200:
  print "Ok : identification effectuée"
  cookie_str = resp['set-cookie'].split(';')[0]
  
  # Raccrochage du poste 8003
  resp, content = h.request("http://ws.agiloffice.fr/demo/phone_hangup?phone=8003", "POST", headers = {'Accept':'application/json', 'Cookie':cookie_str})
  if resp.status == 200:
    print "Ok : raccrochage du poste effectué : " + content
  else:
    print "Erreur " + resp['status'] + " : poste non raccroché : " + content
  
  # Récupération de l'url du serveur de push
  resp, content = h.request("http://ws.agiloffice.fr/demo/push_server_url", "GET", headers = {'Accept':'application/json', 'Cookie':cookie_str})
  if resp.status == 200:
    print "Ok : url du push_server récupérée : " + content
  else:
    print "Erreur " + resp['status'] + " : url du push_server non récupérée : " + content
  
  # Envoi d'un fax
  boundary, data = multipart_encode(open('document.pdf'))
  resp, content = h.request("http://ws.agiloffice.fr/demo/send_fax?from=0123456789&to=9876543210", "POST", body = data, \
    headers = {'Accept':'application/json', 'Cookie':cookie_str, 'Content-type':'multipart/form-data; boundary=' + boundary})
  if resp.status == 200:
    print "Ok : fax envoyé : " + content
    fax_id = json.loads(content)['fax_id']

    # Récupération de l'état du fax envoyé
    resp, content = h.request("http://ws.agiloffice.fr/demo/get_fax_status?fax_id=%d" % fax_id, "GET", headers = {'Accept':'application/json', 'Cookie':cookie_str})
    if resp.status == 200:
      print "Ok : statut du fax récupéré : " + content
    else:
      print "Erreur " + resp['status'] + " : statut du fax non récupérée : " + content
  else:
    print "Erreur " + resp['status'] + " : fax non envoyé : " + content
else:
  print "Erreur " + resp['status'] + " : identification échouée : " + content

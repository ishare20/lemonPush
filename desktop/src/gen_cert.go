package main

import (
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"crypto/x509/pkix"
	"encoding/pem"
	"io/ioutil"
	"log"
	"math/big"
	"net"
	"time"
)

func gen() {
	ca := &x509.Certificate{
		SerialNumber: big.NewInt(1653),
		Subject: pkix.Name{
			Organization: []string{"Lemon Push"},
		},
		NotBefore:             time.Now(),
		NotAfter:              time.Now().AddDate(10, 0, 0),
		SubjectKeyId:          []byte{1, 2, 3, 4, 5},
		BasicConstraintsValid: true,
		IsCA:                  true,
		ExtKeyUsage:           []x509.ExtKeyUsage{x509.ExtKeyUsageClientAuth, x509.ExtKeyUsageServerAuth},
		KeyUsage:              x509.KeyUsageDigitalSignature | x509.KeyUsageCertSign,
	}
	privCa, _ := rsa.GenerateKey(rand.Reader, 1024)
	CreateCertificateFile("ca", ca, privCa, ca, nil)
	server := &x509.Certificate{
		SerialNumber: big.NewInt(1658),
		Subject: pkix.Name{
			Organization: []string{"SERVER"},
		},
		NotBefore:    time.Now(),
		NotAfter:     time.Now().AddDate(10, 0, 0),
		SubjectKeyId: []byte{1, 2, 3, 4, 6},
		ExtKeyUsage:  []x509.ExtKeyUsage{x509.ExtKeyUsageClientAuth, x509.ExtKeyUsageServerAuth},
		KeyUsage:     x509.KeyUsageDigitalSignature | x509.KeyUsageCertSign,
	}
	hosts := []string{"localhost", "127.0.0.1"}
	for _, h := range hosts {
		if ip := net.ParseIP(h); ip != nil {
			server.IPAddresses = append(server.IPAddresses, ip)
		} else {
			server.DNSNames = append(server.DNSNames, h)
		}
	}
	privSer, _ := rsa.GenerateKey(rand.Reader, 1024)
	CreateCertificateFile("server", server, privSer, ca, privCa)
	client := &x509.Certificate{
		SerialNumber: big.NewInt(1658),
		Subject: pkix.Name{
			Organization: []string{"CLIENT"},
		},
		NotBefore:    time.Now(),
		NotAfter:     time.Now().AddDate(10, 0, 0),
		SubjectKeyId: []byte{1, 2, 3, 4, 7},
		ExtKeyUsage:  []x509.ExtKeyUsage{x509.ExtKeyUsageClientAuth, x509.ExtKeyUsageServerAuth},
		KeyUsage:     x509.KeyUsageDigitalSignature | x509.KeyUsageCertSign,
	}
	privCli, _ := rsa.GenerateKey(rand.Reader, 1024)
	CreateCertificateFile("client", client, privCli, ca, privCa)
}

func CreateCertificateFile(name string, cert *x509.Certificate, key *rsa.PrivateKey, caCert *x509.Certificate, caKey *rsa.PrivateKey) {
	priv := key
	pub := &priv.PublicKey
	privPm := priv
	if caKey != nil {
		privPm = caKey
	}
	ca_b, err := x509.CreateCertificate(rand.Reader, cert, caCert, pub, privPm)
	if err != nil {
		log.Println("create failed", err)
		return
	}
	ca_f := name + ".pem"
	log.Println("write to pem", ca_f)
	var certificate = &pem.Block{Type: "CERTIFICATE",
		Headers: map[string]string{},
		Bytes:   ca_b}
	ca_b64 := pem.EncodeToMemory(certificate)
	ioutil.WriteFile(ca_f, ca_b64, 0777)

	priv_f := name + ".key"
	priv_b := x509.MarshalPKCS1PrivateKey(priv)
	log.Println("write to key", priv_f)
	ioutil.WriteFile(priv_f, priv_b, 0777)
	var privateKey = &pem.Block{Type: "PRIVATE KEY",
		Headers: map[string]string{},
		Bytes:   priv_b}
	priv_b64 := pem.EncodeToMemory(privateKey)
	ioutil.WriteFile(priv_f, priv_b64, 0777)
}

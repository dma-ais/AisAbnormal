# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

$script = <<SCRIPT
#!/bin/bash
sudo apt-get update
sudo apt-get -y install unzip
sudo apt-get -y install apache2
git clone https://github.com/dma-ais/AisAbnormal.git
cd AisAbnormal
export M2_HOME=/home/vagrant/apache-maven-3.2.1
export PATH=$PATH:$M2_HOME/bin
mkdir data
cd data
wget --progress=bar:force https://s3-eu-west-1.amazonaws.com/dma-vagrant-boxes/data/aisd.h2.db
wget --progress=bar:force https://s3-eu-west-1.amazonaws.com/dma-vagrant-boxes/data/2013H2-grid200-down10.statistics
wget --progress=bar:force https://s3-eu-west-1.amazonaws.com/dma-vagrant-boxes/data/2013H2-grid200-down10.statistics.p
cd ..
mvn -DskipITs clean install
cd ais-ab-web/target/
unzip ais-ab-web-0.1-SNAPSHOT-bundle.zip
cat | sudo tee /etc/init/dma-ais-ab-web.conf << EOF
  author "Thomas Borg Salling <tbsalling@tbsalling.dk>"
  start on filesystem and net-device-up IFACE!=lo
  respawn limit 10 5

  pre-start script
    logger "starting dma-ais-ab-web"
  end script

  post-start script
    logger "stopping dma-ais-ab-web"
  end script

  script
    sudo su - vagrant
    cd /home/vagrant/AisAbnormal/ais-ab-web/target/ais-ab-web-0.1-SNAPSHOT
    ./run-webapp.sh 8080 /home/vagrant/AisAbnormal/data/aisd /home/vagrant/AisAbnormal/data/2013H2-grid200-down10.statistics
  end script
EOF
sudo initctl reload-configuration
echo "Watch application output in /var/log/upstart/dma-ais-ab-web.log"
SCRIPT

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = "dma/devbox"
  config.vm.network "forwarded_port", guest: 8080, host: 8181
  config.vm.provision :shell, :inline => $script
end

package org.tsh.client;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.xmlrules.DigesterLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import java.net.MalformedURLException;

import java.util.Iterator;
import java.util.List;


/**
 * Fecha 08-oct-2003
 *
 * @author jmgarcia
 */
public class Client implements Runnable {
    //Fichero con las reglas de la configuracion
    private static final String FILE_CONFIG_RULES = "rules-client.xml";

    //Fichero de configuracion por defecto
    private static final String FILE_CONFIG = "tsh-client.xml";

    //Objeto para log
    private Log logger = LogFactory.getLog(Client.class);

    //Fichero de configuracion
    private String configFile;

    //Fichero de reglas para la configuracion	
    private String ruleConfigFile;

    /**
     * Ficheros de configuracion por parametro
         * @param configFile Fichero de configuracion
         * @param ruleConfigFile Fichero de reglas para configuracion
         */
    public Client(String configFile, String ruleConfigFile) {
        this.configFile = configFile;
        this.ruleConfigFile = ruleConfigFile;
    }

    /**
     * Ficheros de configuracion por defecto
     */
    public Client() {
        this(FILE_CONFIG, FILE_CONFIG_RULES);
    }

    /**
    * Metodo de entrada al cliente
    *
    * @param args Parametros de la linea de comandos
    */
    public static void main(String[] args) {
        if (args.length != 2) {
            new Client().run();
        } else {
            new Client(args[0], args[1]).run();
        }
    }

    /**
    * Se crea los diferentes sockets para esperar nuevas conexiones
    */
    public void run() {
        //Leer configuracion del cliente
        File inputXMLFile = new File(this.configFile);
        File rules = new File(this.ruleConfigFile);

        try {
            Digester digester = DigesterLoader.createDigester(rules.toURL());
            List services = (List) digester.parse(inputXMLFile);

            if (services != null) {
                Iterator it = services.iterator();

                while (it.hasNext()) {
                    ServiceInfo service = (ServiceInfo) it.next();
                    new Thread(new ServiceManager(service)).start();
                }
            }
        } catch (MalformedURLException e) {
            logger.debug(e);
        } catch (IOException e) {
            logger.debug(e);
        } catch (SAXException e) {
            logger.debug(e);
        }
    }
}

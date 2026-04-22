package org.libertad.vpn.manager;

public class ConfigParser {
    public static String convertToXrayConfig(String link) throws Exception {
        String cleanLink = link.split("#")[0];
        String raw = cleanLink.replace("vless://", "");

        String[] parts = raw.split("@");
        String uuid = parts[0];

        String[] hostAndParams = parts[1].split("\\?");
        String hostPort = hostAndParams[0];

        String[] hp = hostPort.split(":");
        String address = hp[0];
        int port = Integer.parseInt(hp[1]);

        // Defaults
        String network = "tcp";
        String security = "none";

        String path = "/";
        String host = "";

        // TLS
        String sni = "";

        // Reality
        String fp = "chrome";
        String pbk = "";
        String sid = "";
        String flow = "";
        String spx = "/";

        if (hostAndParams.length > 1) {
            String params = hostAndParams[1];

            for (String param : params.split("&")) {
                String[] kv = param.split("=");
                if (kv.length != 2) continue;

                String key = kv[0];
                String value = java.net.URLDecoder.decode(kv[1], "UTF-8");

                switch (key) {
                    // Default
                    case "type":
                        network = value;
                        break;

                    case "security":
                        security = value;
                        break;

                    case "path":
                        path = value;
                        break;

                    case "host":
                        host = value;
                        break;

                    // TLS / Reality SNI
                    case "sni":
                        sni = value;
                        break;

                    // Reality params
                    case "fp":
                        fp = value;
                        break;

                    case "pbk":
                        pbk = value;
                        break;

                    case "sid":
                        sid = value;
                        break;

                    case "flow":
                        flow = value;
                        break;

                    case "spx":
                        spx = value;
                        break;
                }
            }
        }

        // Validation

        if ("reality".equals(security)) {
            if (pbk.isEmpty() || sid.isEmpty() || sni.isEmpty()) {
                throw new Exception("Invalid Reality config: missing pbk/sid/sni");
            }
        }

        // JSON Build

        StringBuilder json = new StringBuilder();

        json.append("{\n")
            .append("  \"inbounds\": [{")
            .append("\"port\":10808,")
            .append("\"protocol\":\"socks\",")
            .append("\"settings\":{\"auth\":\"noauth\",\"udp\":true}")
            .append("}],\n")

            .append("  \"outbounds\": [{\n")
            .append("    \"protocol\":\"vless\",\n")

            .append("    \"settings\": {\n")
            .append("      \"vnext\": [{\n")
            .append("        \"address\": \"").append(address).append("\",\n")
            .append("        \"port\": ").append(port).append(",\n")
            .append("        \"users\": [{\n")
            .append("          \"id\": \"").append(uuid).append("\",\n")
            .append("          \"encryption\": \"none\"");

        if (!flow.isEmpty()) {
            json.append(",\n          \"flow\": \"").append(flow).append("\"");
        }

        json.append("\n        }]\n")
            .append("      }]\n")
            .append("    },\n")

            .append("    \"streamSettings\": {\n")
            .append("      \"network\": \"").append(network).append("\",\n")
            .append("      \"security\": \"").append(security).append("\"");

        // TLS
        if ("tls".equals(security)) {
            json.append(",\n      \"tlsSettings\": {\n")
                .append("        \"serverName\": \"").append(host).append("\"\n")
                .append("      }");
        }

        // Reality
        if ("reality".equals(security)) {
            json.append(",\n      \"realitySettings\": {\n")
                .append("        \"serverName\": \"").append(sni).append("\",\n")
                .append("        \"publicKey\": \"").append(pbk).append("\",\n")
                .append("        \"shortId\": \"").append(sid).append("\",\n")
                .append("        \"fingerprint\": \"").append(fp).append("\",\n")
                .append("        \"spiderX\": \"").append(spx).append("\"\n")
                .append("      }");
        }

        // WS
        if ("ws".equals(network)) {
            json.append(",\n      \"wsSettings\": {\n")
                .append("        \"path\": \"").append(path).append("\",\n")
                .append("        \"headers\": {\n")
                .append("          \"Host\": \"").append(host).append("\"\n")
                .append("        }\n")
                .append("      }");
        }

        json.append("\n    }\n")
            .append("  }]\n")
            .append("}");

        return json.toString();
    }
}

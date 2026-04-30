package org.bobrolend.lib.v2ray.interfaces;


import org.bobrolend.lib.v2ray.utils.V2rayConstants;

public interface Tun2SocksListener {
    void OnTun2SocksHasMassage(V2rayConstants.CORE_STATES tun2SocksState, String newMessage);
}

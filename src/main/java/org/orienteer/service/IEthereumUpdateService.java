package org.orienteer.service;

import com.google.inject.ImplementedBy;
import com.orientechnologies.orient.core.record.impl.ODocument;

@ImplementedBy(EthereumUpdateServiceImpl.class)
public interface IEthereumUpdateService {
    public void init(ODocument config);
    public void destroy();
}

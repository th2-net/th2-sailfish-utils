package com.exactpro.th2.sailfish.utils;

import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.th2.common.grpc.FilterOperation;

public interface IOperationFilter extends IFilter {
    FilterOperation getOperation();
}

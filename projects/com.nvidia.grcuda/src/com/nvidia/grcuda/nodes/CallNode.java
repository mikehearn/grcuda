/*
 * Copyright (c) 2019, NVIDIA CORPORATION. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of NVIDIA CORPORATION nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.nvidia.grcuda.nodes;

import java.util.Optional;
import com.nvidia.grcuda.GrCUDAException;
import com.nvidia.grcuda.GrCUDALanguage;
import com.nvidia.grcuda.functions.Function;
import com.nvidia.grcuda.functions.FunctionTable;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;

public class CallNode extends ExpressionNode {

    @Child private IdentifierNode identifier;
    @Children private ExpressionNode[] argumentNodes;
    @Child private Node executeNode = Message.EXECUTE.createNode();

    public CallNode(IdentifierNode identifier, ExpressionNode[] argumentNodes) {
        this.identifier = identifier;
        this.argumentNodes = argumentNodes;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        String namespace = identifier.getNamespace();
        String functionName = identifier.getIdentifierName();
        FunctionTable table = GrCUDALanguage.getCurrentLanguage().getContext().getFunctionTable();
        Optional<Function> maybeFunction = table.lookupFunction(functionName, namespace);
        if (!maybeFunction.isPresent()) {
            throw new GrCUDAException("function '" + functionName +
                            "' not found in namespace '" + namespace + "'", this);
        }
        Function function = maybeFunction.get();
        Object[] argumentValues = new Object[argumentNodes.length];
        for (int i = 0; i < argumentNodes.length; i++) {
            argumentValues[i] = argumentNodes[i].execute(frame);
        }
        try {
            return ForeignAccess.sendExecute(executeNode, function, argumentValues);
        } catch (ArityException | UnsupportedTypeException | UnsupportedMessageException e) {
            throw new RuntimeException((e));
        }
    }
}

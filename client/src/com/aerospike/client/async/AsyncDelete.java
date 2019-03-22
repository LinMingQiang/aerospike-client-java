/*
 * Copyright 2012-2019 Aerospike, Inc.
 *
 * Portions may be licensed to Aerospike, Inc. under one or more contributor
 * license agreements WHICH ARE COMPATIBLE WITH THE APACHE LICENSE, VERSION 2.0.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.aerospike.client.async;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.ResultCode;
import com.aerospike.client.cluster.Cluster;
import com.aerospike.client.cluster.Node;
import com.aerospike.client.cluster.Partition;
import com.aerospike.client.listener.DeleteListener;
import com.aerospike.client.policy.WritePolicy;

public final class AsyncDelete extends AsyncCommand {
	private final DeleteListener listener;
	private final WritePolicy writePolicy;
	private final Key key;
	private final Partition partition;
	private boolean existed;

	public AsyncDelete(Cluster cluster, DeleteListener listener, WritePolicy writePolicy, Key key) {
		super(writePolicy, false, true);
		this.listener = listener;
		this.writePolicy = writePolicy;
		this.key = key;
		this.partition = Partition.write(cluster, writePolicy, key);
	}

	@Override
	protected Node getNode(Cluster cluster) {
		return partition.getNodeWrite(cluster);
	}

	@Override
	protected void writeBuffer() {
		setDelete(writePolicy, key);
	}

	@Override
	protected boolean parseResult() {
		validateHeaderSize();

		int resultCode = dataBuffer[5] & 0xFF;

		if (resultCode == 0) {
			existed = true;
		}
		else {
			if (resultCode == ResultCode.KEY_NOT_FOUND_ERROR) {
				existed = false;
			}
			else {
				throw new AerospikeException(resultCode);
			}
		}
		return true;
	}

	@Override
	protected boolean prepareRetry(boolean timeout) {
		partition.prepareRetryWrite(timeout);
		return true;
	}

	@Override
	protected void onSuccess() {
		if (listener != null) {
			listener.onSuccess(key, existed);
		}
	}

	@Override
	protected void onFailure(AerospikeException e) {
		if (listener != null) {
			listener.onFailure(e);
		}
	}
}

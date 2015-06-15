/**
 * Copyright (c) 2013, impossibl.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of impossibl.com nor the names of its contributors may
 *    be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.impossibl.postgis.system.procs;

import com.impossibl.postgres.system.Context;
import com.impossibl.postgres.system.procs.BinaryDecoder;
import com.impossibl.postgres.system.procs.BinaryEncoder;
import com.impossibl.postgres.system.procs.SimpleProcProvider;
import com.impossibl.postgres.system.procs.TextDecoder;
import com.impossibl.postgres.system.procs.TextEncoder;
import com.impossibl.postgres.types.PrimitiveType;
import com.impossibl.postgres.types.Type;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jts.io.WKTReader;

import io.netty.buffer.ByteBuf;

import java.io.IOException;

public class Geometrys extends SimpleProcProvider {

  public Geometrys() {
    super(new TxtEncoder(), new TxtDecoder(), new BinEncoder(), new BinDecoder(), "geometry_");
  }

  static class TxtEncoder extends TextEncoder {

    @Override
    public Class<?> getInputType() {
      return Geometry.class;
    }

    @Override
    public PrimitiveType getOutputPrimitiveType() {
      return PrimitiveType.Binary;
    }

    @Override
    public void encode(Type type, StringBuilder buffer, Object val, Context context) throws IOException {
      Geometry g = (Geometry) val;
      buffer.append(g.toText());
    }

  }

  static class TxtDecoder extends TextDecoder {

    @Override
    public PrimitiveType getInputPrimitiveType() {
      return PrimitiveType.Binary;
    }

    @Override
    public Class<?> getOutputType() {
      return Geometry.class;
    }

    @Override
    protected Object decode(Type type, Short typeLength, Integer typeModifier, CharSequence buffer, Context context) throws IOException {
      if (buffer == null) {
        return null;
      }
      String wkt = buffer.toString();
      if (wkt.length() < 10) {
        return null;
      }

      WKTReader wktReader = new WKTReader();
      try {
        return wktReader.read(buffer.toString());
      }
      catch (ParseException e) {
        throw new IllegalArgumentException("Failed to parse wkt:" + wkt, e);
      }

    }
  }

  static class BinEncoder extends BinaryEncoder {

    @Override
    public Class<?> getInputType() {
      return Geometry.class;
    }

    @Override
    public PrimitiveType getOutputPrimitiveType() {
      return PrimitiveType.Binary;
    }

    @Override
    protected void encode(Type type, ByteBuf buffer, Object val, Context context) throws IOException {
      if (val == null) {
        buffer.writeInt(-1);
      }
      else {
        WKBWriter wb = new WKBWriter();
        byte[] bytes = wb.write((Geometry) val);
        buffer.writeInt(bytes.length);
        buffer.writeBytes(bytes);
      }
    }
  }

  static class BinDecoder extends BinaryDecoder {

    @Override
    public PrimitiveType getInputPrimitiveType() {
      return PrimitiveType.Binary;
    }

    @Override
    public Class<?> getOutputType() {
      return Geometry.class;
    }

    @Override
    protected Object decode(Type type, Short typelength, Integer typemodifier, ByteBuf buffer, Context context) throws IOException {
      int length = buffer.readInt();
      if (length == -1) {
        return null;
      }
      WKBReader wr = new WKBReader();
      byte[] bytes = new byte[length];
      buffer.readBytes(bytes);
      try {
        return wr.read(bytes);
      }
      catch (ParseException e) {
       throw new IllegalArgumentException("Failed to read WKB", e);
      }
    }
  }

}

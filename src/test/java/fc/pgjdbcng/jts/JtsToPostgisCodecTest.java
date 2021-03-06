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
package fc.pgjdbcng.jts;

import com.impossibl.postgres.jdbc.CodecTest;
import com.impossibl.postgres.jdbc.PGConnectionImpl;
import com.impossibl.postgres.jdbc.TestUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class JtsToPostgisCodecTest extends CodecTest {

  @SuppressWarnings("unused")
  private String geomtype;

  public JtsToPostgisCodecTest(String typeName, String geomtype, Object value) {
    super(typeName, value);
    this.geomtype = geomtype;
  }

  @Parameters(name = "test-{0}-{1}")
  public static Collection<Object[]> data() throws Exception {
    Object[][] scalarTypesData = new Object[][] {
      {"geometry", "point", readWkt("POINT(1.0 2.0 20)")},
      {"geometry", "linestring", readWkt("LINESTRING  (10 10 20,20 20 20, 50 50 50, 34 34 34)")},
      {"geometry", "polygon", readWkt("POLYGON ((10 10 0,20 10 0,20 20 0,20 10 0,10 10 0),(5 5 0,5 6 0,6 6 0,6 5 0,5 5 0))")},
      {"geometry", "multilinestring", readWkt("MULTILINESTRING ((10 10 0,20 10 0,20 20 0,20 10 0,10 10 0),(5 5 0,5 6 0,6 6 0,6 5 0,5 5 0))")},
      {"geometry", "multipolygon", readWkt("MULTIPOLYGON (((10 10 0,20 10 0,20 20 0,20 10 0,10 10 0),(5 5 0,5 6 0,6 6 0,6 5 0,5 5 0)),((10 10 0,20 10 0,20 20 0,20 10 0,10 10 0),(5 5 0,5 6 0,6 6 0,6 5 0,5 5 0)))")},
    };

    List<Object[]> data = new ArrayList<>(Arrays.asList(scalarTypesData));
    data.addAll(JtsToPostgisCodecTest.expandData(scalarTypesData));
    return data;
  }

  private static Geometry readWkt(String wkt) {
    WKTReader wktReader = new WKTReader();
    try {
      return wktReader.read(wkt);
    }
    catch (ParseException e) {
      throw new IllegalArgumentException(e);
    }
  }

  protected static List<Object[]> expandData(Object[][] scalarTypesData) {
    List<Object[]> data = new ArrayList<>();

    // Combine entries with generated ones for array and composite testing
    for (final Object[] scalarTypeData : scalarTypesData) {

      final String typeName = (String) scalarTypeData[0];
      final String testTag = (String) scalarTypeData[1];
      final Object typeValue = scalarTypeData[2];

      // Array entry

      final String arrayTypeName = typeName + "[]";

      data.add(new Object[] {arrayTypeName, testTag, new Object[] {typeValue} });

      // Composite entry

      final String structTypeName = typeName + "struct";

      data.add(new Object[] {structTypeName, testTag, new Maker() {

        @Override
        public Object make(PGConnectionImpl conn) throws SQLException {

          Object value = typeValue;

          String elemTypeName = typeName;

          TestUtil.createType(conn, structTypeName, "elem " + elemTypeName);

          conn.getRegistry().unloadType(structTypeName);

          try {
            return conn.createStruct(structTypeName, new Object[] {value });
          }
          catch (SQLException e) {
            throw new RuntimeException(e);
          }
        }

      } });
    }

    return data;
  }


}

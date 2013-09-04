package redep;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.ModelControllerClient.Factory;
import org.jboss.dmr.ModelNode;
import org.mitre.dsmiley.httpproxy.ProxyServlet;

@WebServlet(urlPatterns = "/*", initParams = @WebInitParam(name = ProxyServlet.P_TARGET_URI, value = "http://localhost:9000"))
@SuppressWarnings("all")
public class Redeployer extends ProxyServlet {
  private final static Logger log = new Function0<Logger>() {
    public Logger apply() {
      String _name = Redeployer.class.getName();
      Logger _logger = Logger.getLogger(_name);
      return _logger;
    }
  }.apply();
  
  private String target = null;
  
  private ModelControllerClient client = null;
  
  public void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    try {
      boolean _equals = Objects.equal(this.client, null);
      if (_equals) {
        InetAddress _byName = InetAddress.getByName("localhost");
        ModelControllerClient _create = Factory.create(_byName, 9999);
        this.client = _create;
      }
      boolean _equals_1 = Objects.equal(this.target, null);
      if (_equals_1) {
        ModelNode _modelNode = new ModelNode();
        final Procedure1<ModelNode> _function = new Procedure1<ModelNode>() {
            public void apply(final ModelNode it) {
              ModelNode _get = it.get("operation");
              _get.set("read-children-resources");
              ModelNode _get_1 = it.get("child-type");
              _get_1.set("deployment");
            }
          };
        ModelNode _doubleArrow = ObjectExtensions.<ModelNode>operator_doubleArrow(_modelNode, _function);
        ModelNode result = this.client.execute(_doubleArrow);
        ModelNode _get = result.get("result");
        List<ModelNode> _asList = _get.asList();
        final Function1<ModelNode,String> _function_1 = new Function1<ModelNode,String>() {
            public String apply(final ModelNode it) {
              Set<String> _keys = it.keys();
              String _head = IterableExtensions.<String>head(_keys);
              return _head;
            }
          };
        List<String> _map = ListExtensions.<ModelNode, String>map(_asList, _function_1);
        final Function1<String,String> _function_2 = new Function1<String,String>() {
            public String apply(final String it) {
              String _replaceFirst = it.replaceFirst(".war$", "");
              return _replaceFirst;
            }
          };
        List<String> _map_1 = ListExtensions.<String, String>map(_map, _function_2);
        final Function1<String,Boolean> _function_3 = new Function1<String,Boolean>() {
            public Boolean apply(final String it) {
              boolean _notEquals = (!Objects.equal(it, "redep"));
              return Boolean.valueOf(_notEquals);
            }
          };
        final Iterable<String> wars = IterableExtensions.<String>filter(_map_1, _function_3);
        int _size = IterableExtensions.size(wars);
        boolean _notEquals = (_size != 1);
        if (_notEquals) {
          response.setContentType("text/plain");
          PrintWriter _writer = response.getWriter();
          _writer.println("Place deploy one and only one .war application (besides redep.war)");
          return;
        } else {
          String _head = IterableExtensions.<String>head(wars);
          this.target = _head;
        }
      }
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("http://");
      String _serverName = request.getServerName();
      _builder.append(_serverName, "");
      _builder.append(":");
      int _serverPort = request.getServerPort();
      _builder.append(_serverPort, "");
      _builder.append("/");
      _builder.append(this.target, "");
      URI _uRI = new URI(_builder.toString());
      this.targetUri = _uRI;
      String _property = System.getProperty("jboss.server.base.dir");
      File _file = new File(_property, "deployments");
      File deployments = _file;
      String _plus = (this.target + ".war");
      File _file_1 = new File(deployments, _plus);
      File war = _file_1;
      File _file_2 = new File(war, "WEB-INF/classes");
      File _file_3 = new File(war, "WEB-INF/lib");
      List<File> monitor = Collections.<File>unmodifiableList(Lists.<File>newArrayList(_file_2, _file_3));
      String _plus_1 = (this.target + ".war.redep");
      File _file_4 = new File(deployments, _plus_1);
      File deployed = _file_4;
      long lastDeployed = deployed.lastModified();
      boolean _or = false;
      final List<File> _converted_monitor = (List<File>)monitor;
      boolean _isNewer = this.isNewer(lastDeployed, ((File[])Conversions.unwrapArray(_converted_monitor, File.class)));
      if (_isNewer) {
        _or = true;
      } else {
        String _parameter = request.getParameter("reload");
        boolean _tripleNotEquals = (_parameter != null);
        _or = (_isNewer || _tripleNotEquals);
      }
      if (_or) {
        deployed.createNewFile();
        Date _date = new Date();
        long _time = _date.getTime();
        deployed.setLastModified(_time);
        ModelNode _modelNode_1 = new ModelNode();
        final Procedure1<ModelNode> _function_4 = new Procedure1<ModelNode>() {
            public void apply(final ModelNode it) {
              ModelNode _get = it.get("address");
              String _plus = (Redeployer.this.target + ".war");
              _get.add("deployment", _plus);
              ModelNode _get_1 = it.get("operation");
              _get_1.set("redeploy");
            }
          };
        ModelNode _doubleArrow_1 = ObjectExtensions.<ModelNode>operator_doubleArrow(_modelNode_1, _function_4);
        ModelNode result_1 = this.client.execute(_doubleArrow_1);
        ModelNode _get_1 = result_1.get("outcome");
        String _asString = _get_1.asString();
        boolean _equals_2 = _asString.equals("success");
        boolean _not = (!_equals_2);
        if (_not) {
          response.setContentType("text/plain");
          PrintWriter _writer_1 = response.getWriter();
          StringConcatenation _builder_1 = new StringConcatenation();
          _builder_1.append("Redeployment failed: ");
          ModelNode _get_2 = result_1.get("failure-description");
          String _asString_1 = null;
          if (_get_2!=null) {
            _asString_1=_get_2.asString();
          }
          _builder_1.append(_asString_1, "");
          _builder_1.newLineIfNotEmpty();
          _builder_1.newLine();
          _builder_1.append("Full response: ");
          _builder_1.append(result_1, "");
          _builder_1.newLineIfNotEmpty();
          _writer_1.println(_builder_1);
          return;
        }
        String _plus_2 = ("redeployment finished:" + result_1);
        Redeployer.log.info(_plus_2);
      }
      super.service(request, response);
    } catch (final Throwable _t) {
      if (_t instanceof Exception) {
        final Exception e = (Exception)_t;
        PrintWriter _writer_2 = response.getWriter();
        e.printStackTrace(_writer_2);
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
  }
  
  private boolean isNewer(final long lastDeployed, final File... entries) {
    boolean _xifexpression = false;
    final Function1<File,Boolean> _function = new Function1<File,Boolean>() {
        public Boolean apply(final File it) {
          boolean _isFile = it.isFile();
          return Boolean.valueOf(_isFile);
        }
      };
    Iterable<File> _filter = IterableExtensions.<File>filter(((Iterable<File>)Conversions.doWrapArray(entries)), _function);
    final Function1<File,Boolean> _function_1 = new Function1<File,Boolean>() {
        public Boolean apply(final File it) {
          long _lastModified = it.lastModified();
          boolean _greaterThan = (_lastModified > lastDeployed);
          return Boolean.valueOf(_greaterThan);
        }
      };
    boolean _exists = IterableExtensions.<File>exists(_filter, _function_1);
    if (_exists) {
      _xifexpression = true;
    } else {
      final Function1<File,Boolean> _function_2 = new Function1<File,Boolean>() {
          public Boolean apply(final File it) {
            boolean _isDirectory = it.isDirectory();
            return Boolean.valueOf(_isDirectory);
          }
        };
      Iterable<File> _filter_1 = IterableExtensions.<File>filter(((Iterable<File>)Conversions.doWrapArray(entries)), _function_2);
      final Function1<File,File[]> _function_3 = new Function1<File,File[]>() {
          public File[] apply(final File it) {
            File[] _listFiles = it.listFiles();
            return _listFiles;
          }
        };
      Iterable<File[]> _map = IterableExtensions.<File, File[]>map(_filter_1, _function_3);
      final Function1<File[],Boolean> _function_4 = new Function1<File[],Boolean>() {
          public Boolean apply(final File[] it) {
            boolean _isNewer = Redeployer.this.isNewer(lastDeployed, it);
            return Boolean.valueOf(_isNewer);
          }
        };
      boolean _exists_1 = IterableExtensions.<File[]>exists(_map, _function_4);
      _xifexpression = _exists_1;
    }
    return _xifexpression;
  }
}

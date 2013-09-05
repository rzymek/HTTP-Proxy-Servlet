package redep;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
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
import javax.servlet.ServletContext;
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
      String _parameter = request.getParameter("redep-reset");
      boolean _notEquals = (!Objects.equal(_parameter, null));
      if (_notEquals) {
        this.client = null;
        this.target = null;
      }
      boolean _equals = Objects.equal(this.client, null);
      if (_equals) {
        InetAddress _byName = InetAddress.getByName("localhost");
        ModelControllerClient _create = Factory.create(_byName, 9999);
        this.client = _create;
      }
      final String targetParam = request.getParameter("redep-target");
      boolean _notEquals_1 = (!Objects.equal(targetParam, null));
      if (_notEquals_1) {
        this.target = targetParam;
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
        final Function1<ModelNode,Iterable<ModelNode>> _function_1 = new Function1<ModelNode,Iterable<ModelNode>>() {
          public Iterable<ModelNode> apply(final ModelNode node) {
            Set<String> _keys = node.keys();
            final Function1<String,ModelNode> _function = new Function1<String,ModelNode>() {
              public ModelNode apply(final String it) {
                ModelNode _get = node.get(it);
                return _get;
              }
            };
            Iterable<ModelNode> _map = IterableExtensions.<String, ModelNode>map(_keys, _function);
            return _map;
          }
        };
        List<Iterable<ModelNode>> _map = ListExtensions.<ModelNode, Iterable<ModelNode>>map(_asList, _function_1);
        Iterable<ModelNode> _flatten = Iterables.<ModelNode>concat(_map);
        final Function1<ModelNode,Boolean> _function_2 = new Function1<ModelNode,Boolean>() {
          public Boolean apply(final ModelNode it) {
            ModelNode _get = it.get("content");
            List<ModelNode> _asList = _get.asList();
            int _size = _asList.size();
            boolean _equals = (_size == 1);
            return Boolean.valueOf(_equals);
          }
        };
        Iterable<ModelNode> _filter = IterableExtensions.<ModelNode>filter(_flatten, _function_2);
        final Function1<ModelNode,Boolean> _function_3 = new Function1<ModelNode,Boolean>() {
          public Boolean apply(final ModelNode it) {
            ModelNode _get = it.get("content");
            ModelNode _get_1 = _get.get(0);
            ModelNode _get_2 = _get_1.get("archive");
            boolean _asBoolean = _get_2.asBoolean();
            boolean _not = (!_asBoolean);
            return Boolean.valueOf(_not);
          }
        };
        Iterable<ModelNode> _filter_1 = IterableExtensions.<ModelNode>filter(_filter, _function_3);
        final Function1<ModelNode,Boolean> _function_4 = new Function1<ModelNode,Boolean>() {
          public Boolean apply(final ModelNode it) {
            ModelNode _get = it.get("subsystem");
            Set<String> _keys = _get.keys();
            boolean _contains = _keys.contains("web");
            return Boolean.valueOf(_contains);
          }
        };
        Iterable<ModelNode> _filter_2 = IterableExtensions.<ModelNode>filter(_filter_1, _function_4);
        final Function1<ModelNode,String> _function_5 = new Function1<ModelNode,String>() {
          public String apply(final ModelNode it) {
            ModelNode _get = it.get("name");
            String _asString = _get.asString();
            String _replaceFirst = _asString.replaceFirst("[.]war$", "");
            return _replaceFirst;
          }
        };
        Iterable<String> _map_1 = IterableExtensions.<ModelNode, String>map(_filter_2, _function_5);
        final Function1<String,Boolean> _function_6 = new Function1<String,Boolean>() {
          public Boolean apply(final String it) {
            ServletContext _servletContext = Redeployer.this.getServletContext();
            String _contextPath = _servletContext.getContextPath();
            String _plus = ("/" + it);
            boolean _equals = _contextPath.equals(_plus);
            boolean _not = (!_equals);
            return Boolean.valueOf(_not);
          }
        };
        final Iterable<String> wars = IterableExtensions.<String>filter(_map_1, _function_6);
        boolean _isEmpty = IterableExtensions.isEmpty(wars);
        if (_isEmpty) {
          response.setContentType("text/html");
          PrintWriter _writer = response.getWriter();
          StringConcatenation _builder = new StringConcatenation();
          _builder.append("<html><head><title>Redeployer</title></head><body>");
          _builder.newLine();
          _builder.append("<h3>No other exploaded web applications deployed.</h3><br/><br/>");
          _builder.newLine();
          _builder.append("Here are all the deployments:<ul>");
          _builder.newLine();
          {
            ModelNode _get_1 = result.get("result");
            List<ModelNode> _asList_1 = _get_1.asList();
            for(final ModelNode dep : _asList_1) {
              _builder.append("<li><pre>");
              _builder.append(dep, "");
              _builder.append("</pre></li>");
              _builder.newLineIfNotEmpty();
            }
          }
          _builder.append("</ul></body></html>");
          _writer.println(_builder);
          return;
        } else {
          int _size = IterableExtensions.size(wars);
          boolean _notEquals_2 = (_size != 1);
          if (_notEquals_2) {
            response.setContentType("text/html");
            PrintWriter _writer_1 = response.getWriter();
            StringConcatenation _builder_1 = new StringConcatenation();
            _builder_1.append("<html><head><title>Redeployer</title></head><body>");
            _builder_1.newLine();
            _builder_1.append("Select web application for on-refresh-redeployment:<br/><ul>");
            _builder_1.newLine();
            {
              for(final String war : wars) {
                _builder_1.append("<li><a href=\"?redep-target=");
                _builder_1.append(war, "");
                _builder_1.append("\">");
                _builder_1.append(war, "");
                _builder_1.append("</a><br/></li>");
                _builder_1.newLineIfNotEmpty();
              }
            }
            _builder_1.append("</ul></body></html>");
            _writer_1.println(_builder_1);
            return;
          } else {
            String _head = IterableExtensions.<String>head(wars);
            this.target = _head;
          }
        }
      }
      StringConcatenation _builder_2 = new StringConcatenation();
      _builder_2.append("http://");
      String _serverName = request.getServerName();
      _builder_2.append(_serverName, "");
      _builder_2.append(":");
      int _serverPort = request.getServerPort();
      _builder_2.append(_serverPort, "");
      _builder_2.append("/");
      _builder_2.append(this.target, "");
      URI _uRI = new URI(_builder_2.toString());
      this.targetUri = _uRI;
      String _property = System.getProperty("jboss.server.base.dir");
      File _file = new File(_property, "deployments");
      File deployments = _file;
      String _plus = (this.target + ".war");
      File _file_1 = new File(deployments, _plus);
      File war_1 = _file_1;
      File _file_2 = new File(war_1, "WEB-INF/classes");
      File _file_3 = new File(war_1, "WEB-INF/lib");
      List<File> monitor = Collections.<File>unmodifiableList(Lists.<File>newArrayList(_file_2, _file_3));
      Long _elvis = null;
      ServletContext _servletContext = this.getServletContext();
      String _plus_1 = (this.target + ".lastDeployed");
      Object _attribute = _servletContext.getAttribute(_plus_1);
      if (((Long) _attribute) != null) {
        _elvis = ((Long) _attribute);
      } else {
        Long _valueOf = Long.valueOf(0);
        _elvis = ObjectExtensions.<Long>operator_elvis(((Long) _attribute), _valueOf);
      }
      final Long lastDeployed = _elvis;
      boolean _or = false;
      final List<File> _converted_monitor = (List<File>)monitor;
      boolean _isNewer = this.isNewer((lastDeployed).longValue(), ((File[])Conversions.unwrapArray(_converted_monitor, File.class)));
      if (_isNewer) {
        _or = true;
      } else {
        String _parameter_1 = request.getParameter("redep");
        boolean _tripleNotEquals = (_parameter_1 != null);
        _or = (_isNewer || _tripleNotEquals);
      }
      if (_or) {
        ServletContext _servletContext_1 = this.getServletContext();
        String _plus_2 = (this.target + ".lastDeployed");
        Date _date = new Date();
        long _time = _date.getTime();
        _servletContext_1.setAttribute(_plus_2, Long.valueOf(_time));
        ModelNode _modelNode_1 = new ModelNode();
        final Procedure1<ModelNode> _function_7 = new Procedure1<ModelNode>() {
          public void apply(final ModelNode it) {
            ModelNode _get = it.get("address");
            String _plus = (Redeployer.this.target + ".war");
            _get.add("deployment", _plus);
            ModelNode _get_1 = it.get("operation");
            _get_1.set("redeploy");
          }
        };
        ModelNode _doubleArrow_1 = ObjectExtensions.<ModelNode>operator_doubleArrow(_modelNode_1, _function_7);
        ModelNode result_1 = this.client.execute(_doubleArrow_1);
        ModelNode _get_2 = result_1.get("outcome");
        String _asString = _get_2.asString();
        boolean _equals_2 = _asString.equals("success");
        boolean _not = (!_equals_2);
        if (_not) {
          response.setContentType("text/plain");
          PrintWriter _writer_2 = response.getWriter();
          StringConcatenation _builder_3 = new StringConcatenation();
          _builder_3.append("Redeployment failed: ");
          ModelNode _get_3 = result_1.get("failure-description");
          String _asString_1 = null;
          if (_get_3!=null) {
            _asString_1=_get_3.asString();
          }
          _builder_3.append(_asString_1, "");
          _builder_3.newLineIfNotEmpty();
          _builder_3.newLine();
          _builder_3.append("Full response: ");
          _builder_3.append(result_1, "");
          _builder_3.newLineIfNotEmpty();
          _writer_2.println(_builder_3);
          return;
        }
        String _plus_3 = ("redeployment finished:" + result_1);
        Redeployer.log.info(_plus_3);
      }
      super.service(request, response);
    } catch (final Throwable _t) {
      if (_t instanceof Exception) {
        final Exception e = (Exception)_t;
        PrintWriter _writer_3 = response.getWriter();
        e.printStackTrace(_writer_3);
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

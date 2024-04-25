package com.ibm.wala.cast.python.loader;

import static com.ibm.wala.cast.python.types.PythonTypes.pythonLoader;
import static com.ibm.wala.cast.python.util.Util.getNameStream;
import static java.util.stream.Collectors.toList;

import com.ibm.wala.cast.ir.translator.AstTranslator.AstLexicalInformation;
import com.ibm.wala.cast.ir.translator.AstTranslator.WalkContext;
import com.ibm.wala.cast.ir.translator.TranslatorToIR;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.loader.CAstAbstractModuleLoader;
import com.ibm.wala.cast.python.ir.PythonCAstToIRTranslator;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.CAstTypeDictionaryImpl;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.cast.util.CAstPattern;
import com.ibm.wala.cast.util.CAstPattern.Segments;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class PythonLoader extends CAstAbstractModuleLoader {

  @Override
  public void init(List<Module> modules) {
    // TODO Auto-generated method stub
    super.init(modules);
  }

  public class DynamicMethodBody extends DynamicCodeBody {
    private final IClass container;

    private final Collection<Annotation> annotations;

    public DynamicMethodBody(
        TypeReference codeName,
        TypeReference parent,
        IClassLoader loader,
        Position sourcePosition,
        CAstEntity entity,
        WalkContext context,
        IClass container) {
      super(codeName, parent, loader, sourcePosition, entity, context);
      this.container = container;

      // fill in the decorators.
      // FIXME: Process annotations with parameters.
      this.annotations =
          getNameStream(entity.getAnnotations())
              .map(s -> "L" + s)
              .map(TypeName::findOrCreate)
              .map(tn -> TypeReference.findOrCreate(pythonLoader, tn))
              .map(Annotation::make)
              .collect(toList());
    }

    public IClass getContainer() {
      return container;
    }

    @Override
    public Collection<Annotation> getAnnotations() {
      return this.annotations;
    }
  }

  public class PythonClass extends CoreClass {
    java.util.Set<IField> staticFields = HashSetFactory.make();
    java.util.Set<MethodReference> methodTypes = HashSetFactory.make();
    private java.util.Set<TypeReference> innerTypes = HashSetFactory.make();
    java.util.Set<String> missingTypeNames;

    public PythonClass(
        TypeName name,
        TypeName superName,
        IClassLoader loader,
        Position sourcePosition,
        Set<CAstType> missingTypes) {
      super(name, superName, loader, sourcePosition);
      missingTypeNames = missingTypes.stream().map(t -> t.getName()).collect(Collectors.toSet());
      if (name.toString().lastIndexOf('/') > 0) {
        String maybeOuterName = name.toString().substring(0, name.toString().lastIndexOf('/'));
        TypeName maybeOuter = TypeName.findOrCreate(maybeOuterName);
        if (types.containsKey(maybeOuter)) {
          IClass cls = types.get(maybeOuter);
          if (cls instanceof PythonClass) {
            ((PythonClass) cls).innerTypes.add(this.getReference());
          }
        }
      }
    }

    public java.util.Set<String> getMissingTypeNames() {
      return missingTypeNames;
    }

    @Override
    public Collection<IField> getDeclaredStaticFields() {
      return staticFields;
    }

    public Collection<MethodReference> getMethodReferences() {
      return methodTypes;
    }

    public Collection<TypeReference> getInnerReferences() {
      return innerTypes;
    }
  }

  protected final CAstTypeDictionaryImpl<String> typeDictionary =
      new CAstTypeDictionaryImpl<String>();
  private final CAst Ast = new CAstImpl();
  protected final CAstPattern sliceAssign =
      CAstPattern.parse("<top>ASSIGN(CALL(VAR(\"slice\"),<args>**),<value>*)");

  @Override
  public ClassLoaderReference getReference() {
    return PythonTypes.pythonLoader;
  }

  @Override
  public Language getLanguage() {
    return PythonLanguage.Python;
  }

  @Override
  public SSAInstructionFactory getInstructionFactory() {
    return getLanguage().instructionFactory();
  }

  protected final CAstPattern sliceAssignOp =
      CAstPattern.parse("<top>ASSIGN_POST_OP(CALL(VAR(\"slice\"),<args>**),<value>*,<op>*)");
  final CoreClass Root = new CoreClass(PythonTypes.rootTypeName, null, this, null);
  final CoreClass Exception =
      new CoreClass(PythonTypes.Exception.getName(), PythonTypes.rootTypeName, this, null);

  protected CAstNode rewriteSubscriptAssign(Segments s) {
    int i = 0;
    CAstNode[] args = new CAstNode[s.getMultiple("args").size() + 1];
    for (CAstNode arg : s.getMultiple("args")) {
      args[i++] = arg;
    }
    args[i++] = s.getSingle("value");

    return Ast.makeNode(CAstNode.CALL, Ast.makeNode(CAstNode.VAR, Ast.makeConstant("slice")), args);
  }

  protected CAstNode rewriteSubscriptAssignOp(Segments s) {
    int i = 0;
    CAstNode[] args = new CAstNode[s.getMultiple("args").size() + 1];
    for (CAstNode arg : s.getMultiple("args")) {
      args[i++] = arg;
    }
    args[i++] = s.getSingle("value");

    return Ast.makeNode(CAstNode.CALL, Ast.makeNode(CAstNode.VAR, Ast.makeConstant("slice")), args);
  }

  @Override
  protected boolean shouldTranslate(CAstEntity entity) {
    return true;
  }

  @Override
  protected TranslatorToIR initTranslator(Set<Pair<CAstEntity, ModuleEntry>> topLevelEntities) {
    return new PythonCAstToIRTranslator(this);
  }

  final CoreClass CodeBody =
      new CoreClass(PythonTypes.CodeBody.getName(), PythonTypes.rootTypeName, this, null);
  final CoreClass lambda =
      new CoreClass(PythonTypes.lambda.getName(), PythonTypes.CodeBody.getName(), this, null);
  final CoreClass filter =
      new CoreClass(PythonTypes.filter.getName(), PythonTypes.CodeBody.getName(), this, null);
  final CoreClass comprehension =
      new CoreClass(
          PythonTypes.comprehension.getName(), PythonTypes.CodeBody.getName(), this, null);
  final CoreClass object =
      new CoreClass(PythonTypes.object.getName(), PythonTypes.rootTypeName, this, null);
  final CoreClass list =
      new CoreClass(PythonTypes.list.getName(), PythonTypes.object.getName(), this, null);
  final CoreClass set =
      new CoreClass(PythonTypes.set.getName(), PythonTypes.object.getName(), this, null);
  final CoreClass dict =
      new CoreClass(PythonTypes.dict.getName(), PythonTypes.object.getName(), this, null);
  final CoreClass tuple =
      new CoreClass(PythonTypes.tuple.getName(), PythonTypes.object.getName(), this, null);
  final CoreClass string =
      new CoreClass(PythonTypes.string.getName(), PythonTypes.object.getName(), this, null);
  final CoreClass trampoline =
      new CoreClass(PythonTypes.trampoline.getName(), PythonTypes.CodeBody.getName(), this, null);
  final CoreClass superfun =
      new CoreClass(PythonTypes.superfun.getName(), PythonTypes.CodeBody.getName(), this, null);
  final CoreClass iterator =
      new CoreClass(PythonTypes.iterator.getName(), PythonTypes.object.getName(), this, null);

  /**
   * The <a href="https://docs.python.org/3/using/cmdline.html#envvar-PYTHONPATH">PYTHONPATH</a> to
   * use in the analysis.
   *
   * @apiNote PYTHONPATH is currently only supported for Python 3.
   * @see https://docs.python.org/3/tutorial/modules.html#the-module-search-path.
   */
  protected List<File> pythonPath;

  public PythonLoader(IClassHierarchy cha, IClassLoader parent) {
    super(cha, parent);
  }

  public PythonLoader(IClassHierarchy cha, IClassLoader parent, List<File> pythonPath) {
    super(cha, parent);
    this.pythonPath = pythonPath;
  }

  public PythonLoader(IClassHierarchy cha) {
    super(cha);
  }

  public PythonLoader(IClassHierarchy cha, List<File> pythonPath) {
    super(cha);
    this.pythonPath = pythonPath;
  }

  public IClass makeMethodBodyType(
      String name,
      TypeReference P,
      CAstSourcePositionMap.Position sourcePosition,
      CAstEntity entity,
      WalkContext context,
      IClass container) {
    return new DynamicMethodBody(
        TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.string2TypeName(name)),
        P,
        this,
        sourcePosition,
        entity,
        context,
        container);
  }

  public IClass makeCodeBodyType(
      String name,
      TypeReference P,
      CAstSourcePositionMap.Position sourcePosition,
      CAstEntity entity,
      WalkContext context) {
    return new DynamicCodeBody(
        TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.string2TypeName(name)),
        P,
        this,
        sourcePosition,
        entity,
        context);
  }

  public IClass defineFunctionType(
      String name, CAstSourcePositionMap.Position pos, CAstEntity entity, WalkContext context) {
    CAstType st = entity.getType().getSupertypes().iterator().next();
    return makeCodeBodyType(
        name,
        lookupClass(TypeName.findOrCreate("L" + st.getName())).getReference(),
        pos,
        entity,
        context);
  }

  public IClass defineMethodType(
      String name,
      CAstSourcePositionMap.Position pos,
      CAstEntity entity,
      TypeName typeName,
      WalkContext context) {
    PythonClass self = (PythonClass) types.get(typeName);

    IClass fun = makeMethodBodyType(name, PythonTypes.CodeBody, pos, entity, context, self);

    assert types.containsKey(typeName);

    // Includes static methods.
    MethodReference me =
        MethodReference.findOrCreate(
            fun.getReference(),
            Atom.findOrCreateUnicodeAtom(entity.getType().getName()),
            AstMethodReference.fnDesc);
    self.methodTypes.add(me);

    return fun;
  }

  public IMethod defineCodeBodyCode(
      String clsName,
      AbstractCFG<?, ?> cfg,
      SymbolTable symtab,
      boolean hasCatchBlock,
      Map<IBasicBlock<SSAInstruction>, TypeReference[]> caughtTypes,
      boolean hasMonitorOp,
      AstLexicalInformation lexicalInfo,
      DebuggingInformation debugInfo,
      int defaultArgs) {
    DynamicCodeBody C = (DynamicCodeBody) lookupClass(clsName, cha);
    assert C != null : clsName;
    return C.setCodeBody(
        makeCodeBodyCode(
            cfg,
            symtab,
            hasCatchBlock,
            caughtTypes,
            hasMonitorOp,
            lexicalInfo,
            debugInfo,
            C,
            defaultArgs));
  }

  public DynamicMethodObject makeCodeBodyCode(
      AbstractCFG<?, ?> cfg,
      SymbolTable symtab,
      boolean hasCatchBlock,
      Map<IBasicBlock<SSAInstruction>, TypeReference[]> caughtTypes,
      boolean hasMonitorOp,
      AstLexicalInformation lexicalInfo,
      DebuggingInformation debugInfo,
      IClass C,
      int defaultArgs) {
    return new DynamicMethodObject(
        C,
        Collections.emptySet(),
        cfg,
        symtab,
        hasCatchBlock,
        caughtTypes,
        hasMonitorOp,
        lexicalInfo,
        debugInfo) {
      @Override
      public int getNumberOfDefaultParameters() {
        return defaultArgs;
      }
    };
  }

  public void defineType(
      TypeName cls, TypeName parent, Position sourcePosition, Set<CAstType> missingTypes) {
    new PythonClass(cls, parent, this, sourcePosition, missingTypes);
  }

  public void defineField(TypeName cls, CAstEntity field) {
    assert types.containsKey(cls);
    ((PythonClass) types.get(cls))
        .staticFields.add(
            new IField() {
              @Override
              public String toString() {
                return "field:" + getName();
              }

              @Override
              public IClass getDeclaringClass() {
                return types.get(cls);
              }

              @Override
              public Atom getName() {
                return Atom.findOrCreateUnicodeAtom(field.getName());
              }

              @Override
              public Collection<Annotation> getAnnotations() {
                return Collections.emptySet();
              }

              @Override
              public IClassHierarchy getClassHierarchy() {
                return cha;
              }

              @Override
              public TypeReference getFieldTypeReference() {
                return PythonTypes.Root;
              }

              @Override
              public FieldReference getReference() {
                return FieldReference.findOrCreate(
                    getDeclaringClass().getReference(), getName(), getFieldTypeReference());
              }

              @Override
              public boolean isFinal() {
                return false;
              }

              @Override
              public boolean isPrivate() {
                return false;
              }

              @Override
              public boolean isProtected() {
                return false;
              }

              @Override
              public boolean isPublic() {
                return true;
              }

              @Override
              public boolean isStatic() {
                return true;
              }

              @Override
              public boolean isVolatile() {
                return false;
              }
            });
  }

  /**
   * Return this {@link PythonLoader}'s {@link IClassHierarchy}.
   *
   * @return this {@link PythonLoader}'s {@link IClassHierarchy}.
   */
  public IClassHierarchy getClassHierarchy() {
    return this.cha;
  }

  /**
   * Gets the <a
   * href="https://docs.python.org/3/using/cmdline.html#envvar-PYTHONPATH">PYTHONPATH</a> to use in
   * the analysis.
   *
   * @apiNote PYTHONPATH is currently only supported for Python 3.
   * @see https://docs.python.org/3/tutorial/modules.html#the-module-search-path.
   */
  public List<File> getPythonPath() {
    return pythonPath;
  }
}
